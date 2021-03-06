/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.impl.util.NewProjectUtilPlatform;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.util.Consumer;
import com.intellij.util.containers.HashMap;
import com.intellij.util.graph.GraphGenerator;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.newProject.NewProjectDialog;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ui.configuration.ProjectStructureDialog;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 15, 2003
 */
public class ModulesConfigurator implements ModulesProvider, ModuleEditor.ChangeListener {
  private static final Logger LOG = Logger.getInstance("#" + ModulesConfigurator.class.getName());

  private final Project myProject;
  private final List<ModuleEditor> myModuleEditors = new ArrayList<ModuleEditor>();
  private final Comparator<ModuleEditor> myModuleEditorComparator = new Comparator<ModuleEditor>() {
    @Override
    public int compare(ModuleEditor editor1, ModuleEditor editor2) {
      return ModulesAlphaComparator.INSTANCE.compare(editor1.getModule(), editor2.getModule());
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object o) {
      return false;
    }
  };
  private boolean myModified = false;
  private ModifiableModuleModel myModuleModel;
  private boolean myModuleModelCommitted = false;


  private StructureConfigurableContext myContext;
  private final List<ModuleEditor.ChangeListener> myAllModulesChangeListeners = new ArrayList<ModuleEditor.ChangeListener>();

  public ModulesConfigurator(Project project) {
    myProject = project;
    myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();
  }

  public void setContext(final StructureConfigurableContext context) {
    myContext = context;
  }

  @RequiredDispatchThread
  public void disposeUIResources() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        for (final ModuleEditor moduleEditor : myModuleEditors) {
          Disposer.dispose(moduleEditor);
        }
        myModuleEditors.clear();

        myModuleModel.dispose();
      }
    });

  }

  @Override
  @Nonnull
  public Module[] getModules() {
    return myModuleModel.getModules();
  }

  @Override
  @Nullable
  public Module getModule(String name) {
    final Module moduleByName = myModuleModel.findModuleByName(name);
    if (moduleByName != null) {
      return moduleByName;
    }
    return myModuleModel.getModuleToBeRenamed(name); //if module was renamed
  }

  @Nullable
  public ModuleEditor getModuleEditor(Module module) {
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      if (module.equals(moduleEditor.getModule())) {
        return moduleEditor;
      }
    }
    return null;
  }

  @Override
  public ModuleRootModel getRootModel(@Nonnull Module module) {
    return getOrCreateModuleEditor(module).getRootModel();
  }

  public ModuleEditor getOrCreateModuleEditor(Module module) {
    LOG.assertTrue(getModule(module.getName()) != null, "Module has been deleted");
    ModuleEditor editor = getModuleEditor(module);
    if (editor == null) {
      editor = doCreateModuleEditor(module);
    }
    return editor;
  }

  private ModuleEditor doCreateModuleEditor(final Module module) {
    final ModuleEditor moduleEditor = new HeaderHidingTabbedModuleEditor(myProject, this, module);

    myModuleEditors.add(moduleEditor);

    moduleEditor.addChangeListener(this);
    Disposer.register(moduleEditor, new Disposable() {
      @Override
      public void dispose() {
        moduleEditor.removeChangeListener(ModulesConfigurator.this);
      }
    });
    return moduleEditor;
  }


  @RequiredDispatchThread
  public void resetModuleEditors() {
    myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        if (!myModuleEditors.isEmpty()) {
          LOG.error("module editors was not disposed");
          myModuleEditors.clear();
        }
        final Module[] modules = myModuleModel.getModules();
        if (modules.length > 0) {
          for (Module module : modules) {
            getOrCreateModuleEditor(module);
          }
          Collections.sort(myModuleEditors, myModuleEditorComparator);
        }
      }
    });
    myModified = false;
  }

  @Override
  public void moduleStateChanged(final ModifiableRootModel moduleRootModel) {
    for (ModuleEditor.ChangeListener listener : myAllModulesChangeListeners) {
      listener.moduleStateChanged(moduleRootModel);
    }
    myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, moduleRootModel.getModule()));
  }

  public void addAllModuleChangeListener(ModuleEditor.ChangeListener listener) {
    myAllModulesChangeListeners.add(listener);
  }

  public GraphGenerator<ModuleRootModel> createGraphGenerator() {
    final Map<Module, ModuleRootModel> models = new HashMap<Module, ModuleRootModel>();
    for (ModuleEditor moduleEditor : myModuleEditors) {
      models.put(moduleEditor.getModule(), moduleEditor.getRootModel());
    }
    return ModuleCompilerUtil.createGraphGenerator(models);
  }

  @RequiredDispatchThread
  public void apply() throws ConfigurationException {
    // validate content and source roots 
    final Map<VirtualFile, String> contentRootToModuleNameMap = new HashMap<VirtualFile, String>();
    final Map<VirtualFile, VirtualFile> srcRootsToContentRootMap = new HashMap<VirtualFile, VirtualFile>();
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      final ModifiableRootModel rootModel = moduleEditor.getModifiableRootModel();
      final ContentEntry[] contents = rootModel.getContentEntries();
      for (ContentEntry contentEntry : contents) {
        final VirtualFile contentRoot = contentEntry.getFile();
        if (contentRoot == null) {
          continue;
        }
        final String moduleName = moduleEditor.getName();
        final String previousName = contentRootToModuleNameMap.put(contentRoot, moduleName);
        if (previousName != null && !previousName.equals(moduleName)) {
          throw new ConfigurationException(
                  ProjectBundle.message("module.paths.validation.duplicate.content.error", contentRoot.getPresentableUrl(), previousName, moduleName));
        }

        final VirtualFile[] sourceAndTestFiles = contentEntry.getFolderFiles(ContentFolderScopes.all(false));
        for (VirtualFile srcRoot : sourceAndTestFiles) {
          final VirtualFile anotherContentRoot = srcRootsToContentRootMap.put(srcRoot, contentRoot);
          if (anotherContentRoot != null) {
            final String problematicModule;
            final String correctModule;
            if (VfsUtilCore.isAncestor(anotherContentRoot, contentRoot, true)) {
              problematicModule = contentRootToModuleNameMap.get(anotherContentRoot);
              correctModule = contentRootToModuleNameMap.get(contentRoot);
            }
            else {
              problematicModule = contentRootToModuleNameMap.get(contentRoot);
              correctModule = contentRootToModuleNameMap.get(anotherContentRoot);
            }
            throw new ConfigurationException(ProjectBundle.message("module.paths.validation.duplicate.source.root.error", problematicModule,
                                                                   srcRoot.getPresentableUrl(), correctModule));
          }
        }
      }
    }
    // additional validation: directories marked as src roots must belong to the same module as their corresponding content root
    for (Map.Entry<VirtualFile, VirtualFile> entry : srcRootsToContentRootMap.entrySet()) {
      final VirtualFile srcRoot = entry.getKey();
      final VirtualFile correspondingContent = entry.getValue();
      final String expectedModuleName = contentRootToModuleNameMap.get(correspondingContent);

      for (VirtualFile candidateContent = srcRoot;
           candidateContent != null && !candidateContent.equals(correspondingContent);
           candidateContent = candidateContent.getParent()) {
        final String moduleName = contentRootToModuleNameMap.get(candidateContent);
        if (moduleName != null && !moduleName.equals(expectedModuleName)) {
          throw new ConfigurationException(ProjectBundle
                                                   .message("module.paths.validation.source.root.belongs.to.another.module.error", srcRoot.getPresentableUrl(),
                                                            expectedModuleName, moduleName));
        }
      }
    }

    final List<ModifiableRootModel> models = new ArrayList<ModifiableRootModel>(myModuleEditors.size());
    for (ModuleEditor moduleEditor : myModuleEditors) {
      moduleEditor.canApply();
    }

    for (final ModuleEditor moduleEditor : myModuleEditors) {
      final ModifiableRootModel model = moduleEditor.apply();
      if (model != null) {
        models.add(model);
      }
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          final ModifiableRootModel[] rootModels = models.toArray(new ModifiableRootModel[models.size()]);
          ModifiableModelCommitter.multiCommit(rootModels, myModuleModel);
          myModuleModelCommitted = true;
        }
        finally {

          myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();
          myModuleModelCommitted = false;
        }
      }
    });

    myModified = false;
  }

  public void setModified(final boolean modified) {
    myModified = modified;
  }

  public ModifiableModuleModel getModuleModel() {
    return myModuleModel;
  }

  public boolean isModuleModelCommitted() {
    return myModuleModelCommitted;
  }

  public boolean deleteModule(final Module module) {
    ModuleEditor moduleEditor = getModuleEditor(module);
    if (moduleEditor == null) return true;
    return doRemoveModule(moduleEditor);
  }

  @Nullable
  @RequiredDispatchThread
  @SuppressWarnings("unchecked")
  public List<Module> addModule(Component parent, boolean anImport) {
    if (myProject.isDefault()) return null;

    if (anImport) {
      Pair<ModuleImportProvider, ModuleImportContext> pair = runModuleWizard(parent, true);
      if (pair != null) {
        ModuleImportProvider importProvider = pair.getFirst();
        ModuleImportContext importContext = pair.getSecond();
        assert importProvider != null;
        assert importContext != null;

        final ModifiableArtifactModel artifactModel =
                ProjectStructureConfigurable.getInstance(myProject).getArtifactsStructureConfigurable().getModifiableArtifactModel();
        List<Module> commitedModules = importProvider.commit(importContext, myProject, myModuleModel, this, artifactModel);

        ApplicationManager.getApplication().runWriteAction(() -> {
          for (Module module : commitedModules) {
            getOrCreateModuleEditor(module);
          }
        });
        return commitedModules;
      }
    }
    else {
      FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
        @RequiredDispatchThread
        @Override
        public boolean isFileSelectable(VirtualFile file) {
          if (!super.isFileSelectable(file)) {
            return false;
          }
          for (Module module : myModuleModel.getModules()) {
            VirtualFile moduleDir = module.getModuleDir();
            if (moduleDir != null && moduleDir.equals(file)) {
              return false;
            }
          }
          return true;
        }
      };
      fileChooserDescriptor.setTitle(ProjectBundle.message("choose.module.home"));

      final VirtualFile moduleDir = FileChooser.chooseFile(fileChooserDescriptor, myProject, null);

      if (moduleDir == null) {
        return null;
      }

      final NewProjectDialog dialog = new NewProjectDialog(myProject, moduleDir);
      Module newModule;

      if (dialog.showAndGet()) {
        newModule = NewProjectUtilPlatform.doCreate(dialog.getProjectPanel(), myModuleModel, moduleDir, false);
      }
      else {
        newModule = null;
      }

      if (newModule == null) {
        return null;
      }

      ApplicationManager.getApplication().runWriteAction(() -> {
        getOrCreateModuleEditor(newModule);

        Collections.sort(myModuleEditors, myModuleEditorComparator);
      });
      processModuleCountChanged();

      return Collections.singletonList(newModule);
    }
    return null;
  }

  @RequiredDispatchThread
  private Module createModule(final ModuleBuilder builder) {
    final Exception[] ex = new Exception[]{null};
    final Module module = ApplicationManager.getApplication().runWriteAction(new Computable<Module>() {
      @Override
      @SuppressWarnings({"ConstantConditions"})
      public Module compute() {
        try {
          return builder.createModule(myModuleModel);
        }
        catch (Exception e) {
          ex[0] = e;
          return null;
        }
      }
    });
    if (ex[0] != null) {
      Messages.showErrorDialog(ProjectBundle.message("module.add.error.message", ex[0].getMessage()), ProjectBundle.message("module.add.error.title"));
    }
    return module;
  }

  @Nullable
  @RequiredDispatchThread
  public Module addModule(final ModuleBuilder moduleBuilder) {
    final Module module = createModule(moduleBuilder);
    if (module != null) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          getOrCreateModuleEditor(module);
          Collections.sort(myModuleEditors, myModuleEditorComparator);
        }
      });
      processModuleCountChanged();
    }
    return module;
  }

  @Nullable
  Pair<ModuleImportProvider, ModuleImportContext> runModuleWizard(Component dialogParent, boolean anImport) {
    AddModuleWizard wizard;
    if (anImport) {
      wizard = ImportModuleAction.selectFileAndCreateWizard(myProject, dialogParent);
      if (wizard == null) return null;
      if (wizard.getStepCount() == 0) {
        return Pair.create(wizard.getImportProvider(), wizard.getWizardContext().getModuleImportContext(wizard.getImportProvider()));
      }
    }
    else {
      wizard = new AddModuleWizard(dialogParent, myProject, this);
    }
    wizard.show();
    if (wizard.isOK()) {
      final ModuleImportProvider<?> builder = wizard.getImportProvider();
      if (builder instanceof ModuleBuilder) {
        final ModuleBuilder moduleBuilder = (ModuleBuilder)builder;
        if (moduleBuilder.getName() == null) {
          moduleBuilder.setName(wizard.getProjectName());
        }
        if (moduleBuilder.getModuleDirPath() == null) {
          moduleBuilder.setModuleDirPath(wizard.getModuleDirPath());
        }
      }
      if (!builder.validate(myProject, myProject)) {
        return null;
      }
      return Pair.create(wizard.getImportProvider(), wizard.getWizardContext().getModuleImportContext(builder));
    }

    return null;
  }


  private boolean doRemoveModule(@Nonnull ModuleEditor selectedEditor) {

    String question;
    if (myModuleEditors.size() == 1) {
      question = ProjectBundle.message("module.remove.last.confirmation");
    }
    else {
      question = ProjectBundle.message("module.remove.confirmation", selectedEditor.getModule().getName());
    }
    int result = Messages.showYesNoDialog(myProject, question, ProjectBundle.message("module.remove.confirmation.title"), Messages.getQuestionIcon());
    if (result != Messages.YES) {
      return false;
    }
    // do remove
    myModuleEditors.remove(selectedEditor);

    // destroyProcess removed module
    final Module moduleToRemove = selectedEditor.getModule();
    // remove all dependencies on the module that is about to be removed
    List<ModifiableRootModel> modifiableRootModels = new ArrayList<ModifiableRootModel>();
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      final ModifiableRootModel modifiableRootModel = moduleEditor.getModifiableRootModelProxy();
      modifiableRootModels.add(modifiableRootModel);
    }

    // destroyProcess editor
    ModuleDeleteProvider.removeModule(moduleToRemove, null, modifiableRootModels, myModuleModel);
    processModuleCountChanged();
    Disposer.dispose(selectedEditor);

    return true;
  }


  private void processModuleCountChanged() {
    for (ModuleEditor moduleEditor : myModuleEditors) {
      moduleEditor.fireModuleStateChanged();
    }
  }

  public void processModuleCompilerOutputChanged(String baseUrl) {
    for (ModuleEditor moduleEditor : myModuleEditors) {
      moduleEditor.updateCompilerOutputPathChanged(baseUrl, moduleEditor.getName());
    }
  }

  public boolean isModified() {
    if (myModuleModel.isChanged()) {
      return true;
    }
    for (ModuleEditor moduleEditor : myModuleEditors) {
      if (moduleEditor.isModified()) {
        return true;
      }
    }
    return myModified;
  }

  public static boolean showArtifactSettings(@Nonnull Project project, @Nullable final Artifact artifact) {
    final ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
    return ProjectStructureDialog.show(project, new Consumer<ProjectStructureConfigurable>() {
      @Override
      public void consume(ProjectStructureConfigurable config) {
        configurable.select(artifact, true);
      }
    });
  }

  public static boolean showSdkSettings(@Nonnull Project project, @Nonnull final Sdk sdk) {
    final ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
    return ProjectStructureDialog.show(project, new Consumer<ProjectStructureConfigurable>() {
      @Override
      public void consume(ProjectStructureConfigurable config) {
        configurable.select(sdk, true);
      }
    });
  }

  public static boolean showDialog(Project project, @Nullable final String moduleToSelect, @Nullable final String editorNameToSelect) {
    return ProjectStructureDialog.show(project, new Consumer<ProjectStructureConfigurable>() {
      @Override
      public void consume(ProjectStructureConfigurable config) {
        config.select(moduleToSelect, editorNameToSelect, true);
      }
    });
  }

  public void moduleRenamed(Module module, final String oldName, final String name) {
    for (ModuleEditor moduleEditor : myModuleEditors) {
      if (module == moduleEditor.getModule() && Comparing.strEqual(moduleEditor.getName(), oldName)) {
        moduleEditor.setModuleName(name);
        moduleEditor.updateCompilerOutputPathChanged(ProjectStructureConfigurable.getInstance(myProject).getProjectConfigurable().getCompilerOutputUrl(), name);
        myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, module));
        return;
      }
    }
  }

  public StructureConfigurableContext getContext() {
    return myContext;
  }
}
