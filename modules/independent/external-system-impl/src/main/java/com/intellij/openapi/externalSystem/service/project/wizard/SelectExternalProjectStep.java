package com.intellij.openapi.externalSystem.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.options.ConfigurationException;
import consulo.annotations.DeprecationInfo;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * Handles the following responsibilities:
 * <pre>
 * <ul>
 *   <li>allows end user to define external system config file to import from;</li>
 *   <li>processes the input and reacts accordingly - shows error message if the project is invalid or proceeds to the next screen;</li>
 * </ul>
 * </pre>
 *
 * @author Denis Zhdanov
 * @since 8/1/11 4:15 PM
 */
@Deprecated
@DeprecationInfo("Use consulo.externalSystem.service.module.wizard.SelectExternalProjectStep")
public class SelectExternalProjectStep extends AbstractImportFromExternalSystemWizardStep {

  private final JPanel myComponent = new JPanel(new BorderLayout());

  @Nonnull
  private AbstractImportFromExternalSystemControl myControl;

  private boolean myGradleSettingsInitialised;

  public SelectExternalProjectStep(@Nonnull WizardContext context) {
    super(context);
  }

  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Override
  public void updateStep(@Nonnull WizardContext wizardContext) {
    if (!myGradleSettingsInitialised) {
      initExternalProjectSettingsControl();
    }
  }

  @Override
  public void updateDataModel() {
  }

  // TODO den uncomment
  //@Override
  //public String getHelpId() {
  //  return GradleConstants.HELP_TOPIC_IMPORT_SELECT_PROJECT_STEP;
  //}

  @Override
  public boolean validate(@Nonnull WizardContext wizardContext) throws ConfigurationException {
    myControl.apply();
    AbstractExternalProjectImportBuilder builder = (AbstractExternalProjectImportBuilder)getBuilder();
    if (builder == null) {
      return false;
    }
    builder.ensureProjectIsDefined(getWizardContext());
    return true;
  }

  private void initExternalProjectSettingsControl() {
    AbstractExternalProjectImportBuilder builder = (AbstractExternalProjectImportBuilder)getBuilder();
    if (builder == null) {
      return;
    }
    builder.prepare(getWizardContext());
    myControl = builder.getControl(getWizardContext().getProject());
    myComponent.add(myControl.getComponent());
    myGradleSettingsInitialised = true;
  }
}
