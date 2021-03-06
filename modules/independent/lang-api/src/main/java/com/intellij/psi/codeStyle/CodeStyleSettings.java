/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.ExtensionException;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ClassMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CodeStyleSettings extends CommonCodeStyleSettings implements Cloneable, JDOMExternalizable {

  public static final int MAX_RIGHT_MARGIN = 1000;

  private static final Logger LOG = Logger.getInstance("#" + CodeStyleSettings.class.getName());

  private final ClassMap<CustomCodeStyleSettings> myCustomSettings = new ClassMap<CustomCodeStyleSettings>();

  @NonNls private static final String ADDITIONAL_INDENT_OPTIONS = "ADDITIONAL_INDENT_OPTIONS";

  @NonNls private static final String FILETYPE = "fileType";
  private CommonCodeStyleSettingsManager myCommonSettingsManager = new CommonCodeStyleSettingsManager(this);

  public CodeStyleSettings() {
    this(true);
  }

  public CodeStyleSettings(boolean loadExtensions) {
    super(null);
    initTypeToName();
    initImportsByDefault();

    if (loadExtensions) {
      final CodeStyleSettingsProvider[] codeStyleSettingsProviders = Extensions.getExtensions(CodeStyleSettingsProvider.EXTENSION_POINT_NAME);
      for (final CodeStyleSettingsProvider provider : codeStyleSettingsProviders) {
        addCustomSettings(provider.createCustomSettings(this));
      }
    }
  }

  private void initImportsByDefault() {
    PACKAGES_TO_USE_IMPORT_ON_DEMAND.addEntry(new PackageEntry(false, "java.awt", false));
    PACKAGES_TO_USE_IMPORT_ON_DEMAND.addEntry(new PackageEntry(false, "javax.swing", false));
    IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
    IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.BLANK_LINE_ENTRY);
    IMPORT_LAYOUT_TABLE.addEntry(new PackageEntry(false, "javax", true));
    IMPORT_LAYOUT_TABLE.addEntry(new PackageEntry(false, "java", true));
    IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.BLANK_LINE_ENTRY);
    IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
  }

  private void initTypeToName() {
    initGeneralLocalVariable(PARAMETER_TYPE_TO_NAME);
    initGeneralLocalVariable(LOCAL_VARIABLE_TYPE_TO_NAME);
    PARAMETER_TYPE_TO_NAME.addPair("*Exception", "e");
  }

  private static void initGeneralLocalVariable(@NonNls TypeToNameMap map) {
    map.addPair("int", "i");
    map.addPair("byte", "b");
    map.addPair("char", "c");
    map.addPair("long", "l");
    map.addPair("short", "i");
    map.addPair("boolean", "b");
    map.addPair("double", "v");
    map.addPair("float", "v");
    map.addPair("java.lang.Object", "o");
    map.addPair("java.lang.String", "s");
  }

  public void setParentSettings(CodeStyleSettings parent) {
    myParentSettings = parent;
  }

  public CodeStyleSettings getParentSettings() {
    return myParentSettings;
  }

  private void addCustomSettings(CustomCodeStyleSettings settings) {
    if (settings != null) {
      synchronized (myCustomSettings) {
        myCustomSettings.put(settings.getClass(), settings);
      }
    }
  }

  public <T extends CustomCodeStyleSettings> T getCustomSettings(@Nonnull Class<T> aClass) {
    synchronized (myCustomSettings) {
      return (T)myCustomSettings.get(aClass);
    }
  }

  @Override
  public CodeStyleSettings clone() {
    CodeStyleSettings clone = new CodeStyleSettings();
    clone.copyFrom(this);
    return clone;
  }

  private void copyCustomSettingsFrom(@Nonnull CodeStyleSettings from) {
    synchronized (myCustomSettings) {
      myCustomSettings.clear();

      for (final CustomCodeStyleSettings settings : from.getCustomSettingsValues()) {
        addCustomSettings((CustomCodeStyleSettings)settings.clone());
      }

      FIELD_TYPE_TO_NAME.copyFrom(from.FIELD_TYPE_TO_NAME);
      STATIC_FIELD_TYPE_TO_NAME.copyFrom(from.STATIC_FIELD_TYPE_TO_NAME);
      PARAMETER_TYPE_TO_NAME.copyFrom(from.PARAMETER_TYPE_TO_NAME);
      LOCAL_VARIABLE_TYPE_TO_NAME.copyFrom(from.LOCAL_VARIABLE_TYPE_TO_NAME);

      PACKAGES_TO_USE_IMPORT_ON_DEMAND.copyFrom(from.PACKAGES_TO_USE_IMPORT_ON_DEMAND);
      IMPORT_LAYOUT_TABLE.copyFrom(from.IMPORT_LAYOUT_TABLE);

      OTHER_INDENT_OPTIONS.copyFrom(from.OTHER_INDENT_OPTIONS);

      myAdditionalIndentOptions.clear();
      for (Map.Entry<FileType, IndentOptions> optionEntry : from.myAdditionalIndentOptions.entrySet()) {
        IndentOptions options = optionEntry.getValue();
        myAdditionalIndentOptions.put(optionEntry.getKey(), (IndentOptions)options.clone());
      }

      myCommonSettingsManager = from.myCommonSettingsManager.clone(this);
    }
  }

  public void copyFrom(CodeStyleSettings from) {
    copyPublicFields(from, this);

    copyCustomSettingsFrom(from);
  }


  public boolean USE_SAME_INDENTS = false;

  public boolean IGNORE_SAME_INDENTS_FOR_LANGUAGES = false;

  public boolean AUTODETECT_INDENTS = true;

  public final IndentOptions OTHER_INDENT_OPTIONS = new IndentOptions();

  private final Map<FileType, IndentOptions> myAdditionalIndentOptions = new LinkedHashMap<FileType, IndentOptions>();

  private static final String ourSystemLineSeparator = SystemProperties.getLineSeparator();

  /**
   * Line separator. It can be null if choosen line separator is "System-dependent"!
   */
  public String LINE_SEPARATOR;

  /**
   * @return line separator. If choosen line separator is "System-dependent" method returns default separator for this OS.
   */
  public String getLineSeparator() {
    return LINE_SEPARATOR != null ? LINE_SEPARATOR : ourSystemLineSeparator;
  }


//----------------- NAMING CONVENTIONS --------------------

  public String FIELD_NAME_PREFIX = "";
  public String STATIC_FIELD_NAME_PREFIX = "";
  public String PARAMETER_NAME_PREFIX = "";
  public String LOCAL_VARIABLE_NAME_PREFIX = "";

  public String FIELD_NAME_SUFFIX = "";
  public String STATIC_FIELD_NAME_SUFFIX = "";
  public String PARAMETER_NAME_SUFFIX = "";
  public String LOCAL_VARIABLE_NAME_SUFFIX = "";

  public boolean PREFER_LONGER_NAMES = true;

  public final TypeToNameMap FIELD_TYPE_TO_NAME = new TypeToNameMap();
  public final TypeToNameMap STATIC_FIELD_TYPE_TO_NAME = new TypeToNameMap();
  @NonNls public final TypeToNameMap PARAMETER_TYPE_TO_NAME = new TypeToNameMap();
  public final TypeToNameMap LOCAL_VARIABLE_TYPE_TO_NAME = new TypeToNameMap();

  //----------------- 'final' modifier settings -------
  public boolean GENERATE_FINAL_LOCALS = false;
  public boolean GENERATE_FINAL_PARAMETERS = false;

  //----------------- visibility -----------------------------
  public String VISIBILITY = "public";

  //----------------- generate parentheses around method arguments ----------
  public boolean PARENTHESES_AROUND_METHOD_ARGUMENTS = true;

  //----------------- annotations ----------------
  public boolean USE_EXTERNAL_ANNOTATIONS = false;
  public boolean INSERT_OVERRIDE_ANNOTATION = true;

  //----------------- override -------------------
  public boolean REPEAT_SYNCHRONIZED = true;

  //----------------- FUNCTIONAL EXPRESSIONS -----

  public boolean REPLACE_INSTANCEOF = false;
  public boolean REPLACE_CAST = false;
  public boolean REPLACE_NULL_CHECK = true;

  //----------------- IMPORTS --------------------

  public boolean LAYOUT_STATIC_IMPORTS_SEPARATELY = true;
  public boolean USE_FQ_CLASS_NAMES = false;
  public boolean USE_FQ_CLASS_NAMES_IN_JAVADOC = true;
  public boolean USE_SINGLE_CLASS_IMPORTS = true;
  public boolean INSERT_INNER_CLASS_IMPORTS = false;
  public int CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 5;
  public int NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = 3;
  public final PackageEntryTable PACKAGES_TO_USE_IMPORT_ON_DEMAND = new PackageEntryTable();
  public final PackageEntryTable IMPORT_LAYOUT_TABLE = new PackageEntryTable();

//----------------- ORDER OF MEMBERS ------------------

  public int STATIC_FIELDS_ORDER_WEIGHT = 1;
  public int FIELDS_ORDER_WEIGHT = 2;
  public int CONSTRUCTORS_ORDER_WEIGHT = 3;
  public int STATIC_METHODS_ORDER_WEIGHT = 4;
  public int METHODS_ORDER_WEIGHT = 5;
  public int STATIC_INNER_CLASSES_ORDER_WEIGHT = 6;
  public int INNER_CLASSES_ORDER_WEIGHT = 7;

//----------------- WRAPPING ---------------------------
  /**
   * @deprecated Use get/setRightMargin() methods instead.
   */
  @Deprecated
  public int RIGHT_MARGIN = 120;
  /**
   * <b>Do not use this field directly since it doesn't reflect a setting for a specific language which may
   * overwrite this one. Call {@link #isWrapOnTyping(Language)} method instead.</b>
   *
   * @see #WRAP_ON_TYPING
   */
  public boolean WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = false;


  // ---------------------------------- Javadoc formatting options -------------------------
  public boolean ENABLE_JAVADOC_FORMATTING = true;

  /**
   * Align parameter comments to longest parameter name
   */
  public boolean JD_ALIGN_PARAM_COMMENTS = true;

  /**
   * Align exception comments to longest exception name
   */
  public boolean JD_ALIGN_EXCEPTION_COMMENTS = true;

  public boolean JD_ADD_BLANK_AFTER_PARM_COMMENTS = false;
  public boolean JD_ADD_BLANK_AFTER_RETURN = false;
  public boolean JD_ADD_BLANK_AFTER_DESCRIPTION = true;
  public boolean JD_P_AT_EMPTY_LINES = true;

  public boolean JD_KEEP_INVALID_TAGS = true;
  public boolean JD_KEEP_EMPTY_LINES = true;
  public boolean JD_DO_NOT_WRAP_ONE_LINE_COMMENTS = false;

  public boolean JD_USE_THROWS_NOT_EXCEPTION = true;
  public boolean JD_KEEP_EMPTY_PARAMETER = true;
  public boolean JD_KEEP_EMPTY_EXCEPTION = true;
  public boolean JD_KEEP_EMPTY_RETURN = true;


  public boolean JD_LEADING_ASTERISKS_ARE_ENABLED = true;

  public boolean JD_PRESERVE_LINE_FEEDS = false;
  public boolean JD_PARAM_DESCRIPTION_ON_NEW_LINE = false;

  // ---------------------------------------------------------------------------------------

  // ---------------------------------- HTML formatting options -------------------------
  public boolean HTML_KEEP_WHITESPACES = false;
  public int HTML_ATTRIBUTE_WRAP = WRAP_AS_NEEDED;
  public int HTML_TEXT_WRAP = WRAP_AS_NEEDED;

  public boolean HTML_KEEP_LINE_BREAKS = true;
  public boolean HTML_KEEP_LINE_BREAKS_IN_TEXT = true;
  public int HTML_KEEP_BLANK_LINES = 2;

  public boolean HTML_ALIGN_ATTRIBUTES = true;
  public boolean HTML_ALIGN_TEXT = false;

  public boolean HTML_SPACE_AROUND_EQUALITY_IN_ATTRINUTE = false;
  public boolean HTML_SPACE_AFTER_TAG_NAME = false;
  public boolean HTML_SPACE_INSIDE_EMPTY_TAG = false;

  @NonNls public String HTML_ELEMENTS_TO_INSERT_NEW_LINE_BEFORE = "body,div,p,form,h1,h2,h3";
  @NonNls public String HTML_ELEMENTS_TO_REMOVE_NEW_LINE_BEFORE = "br";
  @NonNls public String HTML_DO_NOT_INDENT_CHILDREN_OF = "html,body,thead,tbody,tfoot";
  public int HTML_DO_NOT_ALIGN_CHILDREN_OF_MIN_LINES = 200;

  @NonNls public String HTML_KEEP_WHITESPACES_INSIDE = "span,pre,textarea";
  @NonNls public String HTML_INLINE_ELEMENTS =
          "a,abbr,acronym,b,basefont,bdo,big,br,cite,cite,code,dfn,em,font,i,img,input,kbd,label,q,s,samp,select,span,strike,strong,sub,sup,textarea,tt,u,var";
  @NonNls public String HTML_DONT_ADD_BREAKS_IF_INLINE_CONTENT = "title,h1,h2,h3,h4,h5,h6,p";

  // ---------------------------------------------------------------------------------------


  // true if <%page import="x.y.z, x.y.t"%>
  // false if <%page import="x.y.z"%>
  //          <%page import="x.y.t"%>
  public boolean JSP_PREFER_COMMA_SEPARATED_IMPORT_LIST = false;

  //----------------------------------------------------------------------------------------

  // region Formatter control

  public boolean FORMATTER_TAGS_ENABLED = false;
  public String FORMATTER_ON_TAG = "@formatter:on";
  public String FORMATTER_OFF_TAG = "@formatter:off";

  public volatile boolean FORMATTER_TAGS_ACCEPT_REGEXP = false;
  private volatile Pattern myFormatterOffPattern = null;
  private volatile Pattern myFormatterOnPattern = null;

  @javax.annotation.Nullable
  public Pattern getFormatterOffPattern() {
    if (myFormatterOffPattern == null && FORMATTER_TAGS_ENABLED && FORMATTER_TAGS_ACCEPT_REGEXP) {
      myFormatterOffPattern = getPatternOrDisableRegexp(FORMATTER_OFF_TAG);
    }
    return myFormatterOffPattern;
  }

  public void setFormatterOffPattern(@javax.annotation.Nullable Pattern formatterOffPattern) {
    myFormatterOffPattern = formatterOffPattern;
  }

  @javax.annotation.Nullable
  public Pattern getFormatterOnPattern() {
    if (myFormatterOffPattern == null && FORMATTER_TAGS_ENABLED && FORMATTER_TAGS_ACCEPT_REGEXP) {
      myFormatterOnPattern = getPatternOrDisableRegexp(FORMATTER_ON_TAG);
    }
    return myFormatterOnPattern;
  }

  public void setFormatterOnPattern(@javax.annotation.Nullable Pattern formatterOnPattern) {
    myFormatterOnPattern = formatterOnPattern;
  }

  @Nullable
  private Pattern getPatternOrDisableRegexp(@Nonnull String markerText) {
    try {
      return Pattern.compile(markerText);
    }
    catch (PatternSyntaxException pse) {
      LOG.error("Loaded regexp pattern is invalid: '" + markerText + "', error message: " + pse.getMessage());
      FORMATTER_TAGS_ACCEPT_REGEXP = false;
      return null;
    }
  }


  // endregion

  //----------------------------------------------------------------------------------------

  private CodeStyleSettings myParentSettings;
  private boolean myLoadedAdditionalIndentOptions;

  @Nonnull
  private Collection<CustomCodeStyleSettings> getCustomSettingsValues() {
    synchronized (myCustomSettings) {
      return Collections.unmodifiableCollection(myCustomSettings.values());
    }
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
    if (LAYOUT_STATIC_IMPORTS_SEPARATELY) {
      // add <all other static imports> entry if there is none
      boolean found = false;
      for (PackageEntry entry : IMPORT_LAYOUT_TABLE.getEntries()) {
        if (entry == PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY) {
          found = true;
          break;
        }
      }
      if (!found) {
        PackageEntry last = IMPORT_LAYOUT_TABLE.getEntryCount() == 0 ? null : IMPORT_LAYOUT_TABLE.getEntryAt(IMPORT_LAYOUT_TABLE.getEntryCount() - 1);
        if (last != PackageEntry.BLANK_LINE_ENTRY) {
          IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.BLANK_LINE_ENTRY);
        }
        IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
      }
    }
    for (final CustomCodeStyleSettings settings : getCustomSettingsValues()) {
      settings.readExternal(element);
      settings.importLegacySettings();
    }

    final List<Element> list = element.getChildren(ADDITIONAL_INDENT_OPTIONS);
    if (list != null) {
      for (Element additionalIndentElement : list) {

        final String fileTypeId = additionalIndentElement.getAttributeValue(FILETYPE);

        if (fileTypeId != null && !fileTypeId.isEmpty()) {
          FileType target = FileTypeManager.getInstance().getFileTypeByExtension(fileTypeId);
          if (target == UnknownFileType.INSTANCE || target == PlainTextFileType.INSTANCE || target.getDefaultExtension().isEmpty()) {
            target = new TempFileType(fileTypeId);
          }

          final IndentOptions options = getDefaultIndentOptions(target);
          options.readExternal(additionalIndentElement);
          registerAdditionalIndentOptions(target, options);
        }
      }
    }

    myCommonSettingsManager.readExternal(element);

    if (USE_SAME_INDENTS) IGNORE_SAME_INDENTS_FOR_LANGUAGES = true;
  }


  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    final CodeStyleSettings parentSettings = new CodeStyleSettings();
    DefaultJDOMExternalizer.writeExternal(this, element, new DifferenceFilter<CodeStyleSettings>(this, parentSettings));
    List<CustomCodeStyleSettings> customSettings = new ArrayList<CustomCodeStyleSettings>(getCustomSettingsValues());

    Collections.sort(customSettings, new Comparator<CustomCodeStyleSettings>() {
      @Override
      public int compare(final CustomCodeStyleSettings o1, final CustomCodeStyleSettings o2) {
        return o1.getTagName().compareTo(o2.getTagName());
      }
    });

    for (final CustomCodeStyleSettings settings : customSettings) {
      final CustomCodeStyleSettings parentCustomSettings = parentSettings.getCustomSettings(settings.getClass());
      if (parentCustomSettings == null) {
        throw new WriteExternalException("Custom settings are null for " + settings.getClass());
      }
      settings.writeExternal(element, parentCustomSettings);
    }

    final FileType[] fileTypes = myAdditionalIndentOptions.keySet().toArray(new FileType[myAdditionalIndentOptions.keySet().size()]);
    Arrays.sort(fileTypes, new Comparator<FileType>() {
      @Override
      public int compare(final FileType o1, final FileType o2) {
        return o1.getDefaultExtension().compareTo(o2.getDefaultExtension());
      }
    });

    for (FileType fileType : fileTypes) {
      final IndentOptions indentOptions = myAdditionalIndentOptions.get(fileType);
      Element additionalIndentOptions = new Element(ADDITIONAL_INDENT_OPTIONS);
      indentOptions.serialize(additionalIndentOptions, getDefaultIndentOptions(fileType));
      additionalIndentOptions.setAttribute(FILETYPE, fileType.getDefaultExtension());
      if (!additionalIndentOptions.getChildren().isEmpty()) {
        element.addContent(additionalIndentOptions);
      }
    }

    myCommonSettingsManager.writeExternal(element);
  }


  private static IndentOptions getDefaultIndentOptions(FileType fileType) {
    final FileTypeIndentOptionsProvider[] providers = Extensions.getExtensions(FileTypeIndentOptionsProvider.EP_NAME);
    for (final FileTypeIndentOptionsProvider provider : providers) {
      if (provider.getFileType().equals(fileType)) {
        return getFileTypeIndentOptions(provider);
      }
    }
    return new IndentOptions();
  }

  @Override
  @Nullable
  public IndentOptions getIndentOptions() {
    return OTHER_INDENT_OPTIONS;
  }

  /**
   * If the file type has an associated language and language indent options are defined, returns these options. Otherwise attempts to find
   * indent options from <code>FileTypeIndentOptionsProvider</code>. If none are found, other indent options are returned.
   *
   * @param fileType The file type to search indent options for.
   * @return File type indent options or <code>OTHER_INDENT_OPTIONS</code>.
   * @see FileTypeIndentOptionsProvider
   * @see LanguageCodeStyleSettingsProvider
   */
  public IndentOptions getIndentOptions(@javax.annotation.Nullable FileType fileType) {
    IndentOptions indentOptions = getLanguageIndentOptions(fileType);
    if (indentOptions != null) return indentOptions;

    if (USE_SAME_INDENTS || fileType == null) return OTHER_INDENT_OPTIONS;

    if (!myLoadedAdditionalIndentOptions) {
      loadAdditionalIndentOptions();
    }
    indentOptions = myAdditionalIndentOptions.get(fileType);
    if (indentOptions != null) return indentOptions;

    return OTHER_INDENT_OPTIONS;
  }

  /**
   * If the document has an associated PsiFile, returns options for this file. Otherwise attempts to find associated VirtualFile and
   * return options for corresponding FileType. If none are found, other indent options are returned.
   *
   * @param project  The project in which PsiFile should be searched.
   * @param document The document to search indent options for.
   * @return Indent options from the indent options providers or file type indent options or <code>OTHER_INDENT_OPTIONS</code>.
   * @see FileIndentOptionsProvider
   * @see FileTypeIndentOptionsProvider
   * @see LanguageCodeStyleSettingsProvider
   */
  @Nonnull
  public IndentOptions getIndentOptionsByDocument(@Nullable Project project, @Nonnull Document document) {
    PsiFile file = project != null ? PsiDocumentManager.getInstance(project).getPsiFile(document) : null;
    if (file != null) return getIndentOptionsByFile(file);

    VirtualFile vFile = FileDocumentManager.getInstance().getFile(document);
    FileType fileType = vFile != null ? vFile.getFileType() : null;
    return getIndentOptions(fileType);
  }

  @Nonnull
  public IndentOptions getIndentOptionsByFile(@Nullable PsiFile file) {
    return getIndentOptionsByFile(file, null);
  }

  @Nonnull
  public IndentOptions getIndentOptionsByFile(@javax.annotation.Nullable PsiFile file, @javax.annotation.Nullable TextRange formatRange) {
    return getIndentOptionsByFile(file, formatRange, false, null);
  }

  /**
   * Retrieves indent options for PSI file from an associated document or (if not defined in the document) from file indent options
   * providers.
   *
   * @param file              The PSI file to retrieve options for.
   * @param formatRange       The text range within the file for formatting purposes or null if there is either no specific range or multiple
   *                          ranges. If the range covers the entire file (full reformat), options stored in the document are ignored and
   *                          indent options are taken from file indent options providers.
   * @param ignoreDocOptions  Ignore options stored in the document and use file indent options providers even if there is no text range
   *                          or the text range doesn't cover the entire file.
   * @param providerProcessor A callback object containing a reference to indent option provider which has returned indent options.
   * @return Indent options from the associated document or file indent options providers.
   * @see com.intellij.psi.codeStyle.FileIndentOptionsProvider
   */
  @Nonnull
  public IndentOptions getIndentOptionsByFile(@Nullable PsiFile file,
                                              @javax.annotation.Nullable TextRange formatRange,
                                              boolean ignoreDocOptions,
                                              @javax.annotation.Nullable Processor<FileIndentOptionsProvider> providerProcessor) {
    if (file != null && file.isValid()) {
      boolean isFullReformat = isFileFullyCoveredByRange(file, formatRange);
      if (!ignoreDocOptions && !isFullReformat) {
        IndentOptions options = IndentOptions.retrieveFromAssociatedDocument(file);
        if (options != null) {
          FileIndentOptionsProvider provider = options.getFileIndentOptionsProvider();
          if (providerProcessor != null && provider != null) {
            providerProcessor.process(provider);
          }
          return options;
        }
      }

      boolean committedDocumentNeeded = false;
      for (FileIndentOptionsProvider provider : Extensions.getExtensions(FileIndentOptionsProvider.EP_NAME)) {
        if (!isFullReformat || provider.useOnFullReformat()) {
          committedDocumentNeeded |= provider instanceof ProviderForCommittedDocument;
          IndentOptions indentOptions = provider.getIndentOptions(this, file);
          if (indentOptions != null) {
            if (providerProcessor != null) {
              providerProcessor.process(provider);
            }
            indentOptions.setFileIndentOptionsProvider(provider);
            logIndentOptions(file, provider, indentOptions);
            return indentOptions;
          }
        }
      }

      IndentOptions options = getIndentOptions(file.getFileType());
      if (committedDocumentNeeded) {
        markOptionsInaccurateIfDocumentUncommitted(options, file);
      }
      return options;
    }
    else {
      return OTHER_INDENT_OPTIONS;
    }
  }

  private static void markOptionsInaccurateIfDocumentUncommitted(@Nonnull IndentOptions options, @Nonnull PsiFile file) {
    PsiDocumentManager manager = PsiDocumentManager.getInstance(file.getProject());
    Document document = manager.getDocument(file);
    if (document != null && !manager.isCommitted(document)) {
      options.setRecalculateForCommittedDocument(true);
    }
  }

  public static boolean isRecalculateForCommittedDocument(@Nonnull IndentOptions options) {
    return options.isRecalculateForCommittedDocument();
  }

  private static boolean isFileFullyCoveredByRange(@Nonnull PsiFile file, @Nullable TextRange formatRange) {
    return formatRange != null && file.getTextRange().equals(formatRange);
  }

  private static void logIndentOptions(@Nonnull PsiFile file, @Nonnull FileIndentOptionsProvider provider, @Nonnull IndentOptions options) {
    LOG.debug("Indent options returned by " + provider.getClass().getName() +
              " for " + file.getName() +
              ": indent size=" + options.INDENT_SIZE +
              ", use tabs=" + options.USE_TAB_CHARACTER +
              ", tab size=" + options.TAB_SIZE);
  }

  @javax.annotation.Nullable
  private IndentOptions getLanguageIndentOptions(@javax.annotation.Nullable FileType fileType) {
    if (fileType == null || !(fileType instanceof LanguageFileType)) return null;
    Language lang = ((LanguageFileType)fileType).getLanguage();
    CommonCodeStyleSettings langSettings = getCommonSettings(lang);
    return langSettings == this ? null : langSettings.getIndentOptions();
  }

  public boolean isSmartTabs(FileType fileType) {
    return getIndentOptions(fileType).SMART_TABS;
  }

  public int getIndentSize(FileType fileType) {
    return getIndentOptions(fileType).INDENT_SIZE;
  }

  public int getContinuationIndentSize(FileType fileType) {
    return getIndentOptions(fileType).CONTINUATION_INDENT_SIZE;
  }

  public int getLabelIndentSize(FileType fileType) {
    return getIndentOptions(fileType).LABEL_INDENT_SIZE;
  }

  public boolean getLabelIndentAbsolute(FileType fileType) {
    return getIndentOptions(fileType).LABEL_INDENT_ABSOLUTE;
  }

  public int getTabSize(FileType fileType) {
    return getIndentOptions(fileType).TAB_SIZE;
  }

  public boolean useTabCharacter(FileType fileType) {
    return getIndentOptions(fileType).USE_TAB_CHARACTER;
  }

  public static class TypeToNameMap implements JDOMExternalizable {
    private final List<String> myPatterns = new ArrayList<String>();
    private final List<String> myNames = new ArrayList<String>();

    public void addPair(String pattern, String name) {
      myPatterns.add(pattern);
      myNames.add(name);
    }

    public String nameByType(String type) {
      for (int i = 0; i < myPatterns.size(); i++) {
        String pattern = myPatterns.get(i);
        if (StringUtil.startsWithChar(pattern, '*')) {
          if (type.endsWith(pattern.substring(1))) {
            return myNames.get(i);
          }
        }
        else {
          if (type.equals(pattern)) {
            return myNames.get(i);
          }
        }
      }
      return null;
    }

    @Override
    public void readExternal(@NonNls Element element) throws InvalidDataException {
      myPatterns.clear();
      myNames.clear();
      for (final Object o : element.getChildren("pair")) {
        @NonNls Element e = (Element)o;

        String pattern = e.getAttributeValue("type");
        String name = e.getAttributeValue("name");
        if (pattern == null || name == null) {
          throw new InvalidDataException();
        }
        myPatterns.add(pattern);
        myNames.add(name);

      }
    }

    @Override
    public void writeExternal(Element parentNode) throws WriteExternalException {
      for (int i = 0; i < myPatterns.size(); i++) {
        String pattern = myPatterns.get(i);
        String name = myNames.get(i);
        @NonNls Element element = new Element("pair");
        parentNode.addContent(element);
        element.setAttribute("type", pattern);
        element.setAttribute("name", name);
      }
    }

    public void copyFrom(TypeToNameMap from) {
      assert from != this;
      myPatterns.clear();
      myPatterns.addAll(from.myPatterns);
      myNames.clear();
      myNames.addAll(from.myNames);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof TypeToNameMap) {
        TypeToNameMap otherMap = (TypeToNameMap)other;
        return myPatterns.equals(otherMap.myPatterns) && myNames.equals(otherMap.myNames);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int code = 0;
      for (String myPattern : myPatterns) {
        code += myPattern.hashCode();
      }
      for (String myName : myNames) {
        code += myName.hashCode();
      }
      return code;
    }
  }

  private void registerAdditionalIndentOptions(FileType fileType, IndentOptions options) {
    boolean exist = false;
    for (final FileType existing : myAdditionalIndentOptions.keySet()) {
      if (Comparing.strEqual(existing.getDefaultExtension(), fileType.getDefaultExtension())) {
        exist = true;
        break;
      }
    }

    if (!exist) {
      myAdditionalIndentOptions.put(fileType, options);
    }
  }

  public IndentOptions getAdditionalIndentOptions(FileType fileType) {
    if (!myLoadedAdditionalIndentOptions) {
      loadAdditionalIndentOptions();
    }
    return myAdditionalIndentOptions.get(fileType);
  }

  private void loadAdditionalIndentOptions() {
    synchronized (myAdditionalIndentOptions) {
      myLoadedAdditionalIndentOptions = true;
      final FileTypeIndentOptionsProvider[] providers = Extensions.getExtensions(FileTypeIndentOptionsProvider.EP_NAME);
      for (final FileTypeIndentOptionsProvider provider : providers) {
        if (!myAdditionalIndentOptions.containsKey(provider.getFileType())) {
          registerAdditionalIndentOptions(provider.getFileType(), getFileTypeIndentOptions(provider));
        }
      }
    }
  }

  private static IndentOptions getFileTypeIndentOptions(FileTypeIndentOptionsProvider provider) {
    try {
      return provider.createIndentOptions();
    }
    catch (AbstractMethodError error) {
      LOG.error("Plugin uses obsolete API.", new ExtensionException(provider.getClass()));
      return new IndentOptions();
    }
  }

  @TestOnly
  public void clearCodeStyleSettings() {
    CodeStyleSettings cleanSettings = new CodeStyleSettings();
    copyFrom(cleanSettings);
    myAdditionalIndentOptions.clear(); //hack
    myLoadedAdditionalIndentOptions = false;
  }

  private static class TempFileType implements FileType {
    private final String myExtension;

    private TempFileType(@Nonnull final String extension) {
      myExtension = extension;
    }

    @Override
    @Nonnull
    public String getName() {
      return "TempFileType";
    }

    @Override
    @Nonnull
    public String getDescription() {
      return "TempFileType";
    }

    @Override
    @Nonnull
    public String getDefaultExtension() {
      return myExtension;
    }

    @Override
    public Icon getIcon() {
      return null;
    }

    @Override
    public boolean isBinary() {
      return false;
    }

    @Override
    public boolean isReadOnly() {
      return false;
    }

    @Override
    public String getCharset(@Nonnull VirtualFile file, @Nonnull byte[] content) {
      return null;
    }
  }

  public CommonCodeStyleSettings getCommonSettings(Language lang) {
    return myCommonSettingsManager.getCommonSettings(lang);
  }

  /**
   * @param langName The language name.
   * @return Language-specific code style settings or shared settings if not found.
   * @see CommonCodeStyleSettingsManager#getCommonSettings
   */
  public CommonCodeStyleSettings getCommonSettings(String langName) {
    return myCommonSettingsManager.getCommonSettings(langName);
  }

  /**
   * Retrieves right margin for the given language. The language may overwrite default RIGHT_MARGIN value with its own RIGHT_MARGIN
   * in language's CommonCodeStyleSettings instance.
   *
   * @param language The language to get right margin for or null if root (default) right margin is requested.
   * @return The right margin for the language if it is defined (not null) and its settings contain non-negative margin. Root (default)
   * margin otherwise (CodeStyleSettings.RIGHT_MARGIN).
   */
  public int getRightMargin(@javax.annotation.Nullable Language language) {
    if (language != null) {
      CommonCodeStyleSettings langSettings = getCommonSettings(language);
      if (langSettings != null) {
        if (langSettings.RIGHT_MARGIN >= 0) return langSettings.RIGHT_MARGIN;
      }
    }
    return getDefaultRightMargin();
  }

  /**
   * Assigns another right margin for the language or (if it is null) to root (default) margin.
   *
   * @param language    The language to assign the right margin to or null if root (default) right margin is to be changed.
   * @param rightMargin New right margin.
   */
  public void setRightMargin(@javax.annotation.Nullable Language language, int rightMargin) {
    if (language != null) {
      CommonCodeStyleSettings langSettings = getCommonSettings(language);
      if (langSettings != null) {
        langSettings.RIGHT_MARGIN = rightMargin;
        return;
      }
    }
    setDefaultRightMargin(rightMargin);
  }

  @SuppressWarnings("deprecation")
  public int getDefaultRightMargin() {
    return RIGHT_MARGIN;
  }

  @SuppressWarnings("deprecation")
  public void setDefaultRightMargin(int rightMargin) {
    RIGHT_MARGIN = rightMargin;
  }

  /**
   * Defines whether or not wrapping should occur when typing reaches right margin.
   *
   * @param language The language to check the option for or null for a global option.
   * @return True if wrapping on right margin is enabled.
   */
  public boolean isWrapOnTyping(@javax.annotation.Nullable Language language) {
    if (language != null) {
      CommonCodeStyleSettings langSettings = getCommonSettings(language);
      if (langSettings != null) {
        if (langSettings.WRAP_ON_TYPING != WrapOnTyping.DEFAULT.intValue) {
          return langSettings.WRAP_ON_TYPING == WrapOnTyping.WRAP.intValue;
        }
      }
    }
    //noinspection deprecation
    return WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN;
  }
}
