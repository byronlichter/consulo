/*
 * @author max
 */
package com.intellij.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiFileSystemItemProcessor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.annotations.RequiredWriteAction;

public abstract class SyntheticFileSystemItem extends PsiElementBase implements PsiFileSystemItem {
  public static final Logger LOG = Logger.getInstance("#" + SyntheticFileSystemItem.class.getPackage().getName());

  protected final Project myProject;
  protected final PsiManager myManager;

  public SyntheticFileSystemItem(Project project) {
    myProject = project;
    myManager = PsiManager.getInstance(myProject);
  }

  protected static boolean processFileSystemItem(PsiElementProcessor<PsiFileSystemItem> processor, PsiFileSystemItem element) {
    if (processor instanceof PsiFileSystemItemProcessor && !((PsiFileSystemItemProcessor)processor).acceptItem(element.getName(), true)) {
      return true;
    }

    return processor.execute(element);
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public ASTNode getNode() {
    return null;
  }

  @Override
  public boolean isPhysical() {
    return true;
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean isValid() {
    final VirtualFile virtualFile = getVirtualFile();
    return virtualFile != null && virtualFile.isValid();
  }

  @Override
  public PsiElement replace(@Nonnull final PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be changed");
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be deleted");
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be deleted");
  }

  @Override
  public void accept(@Nonnull final PsiElementVisitor visitor) {
    // TODO
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    final PsiElementProcessor.CollectElements<PsiFileSystemItem> collector = new PsiElementProcessor.CollectElements<PsiFileSystemItem>();
    processChildren(collector);
    return collector.toArray(new PsiFileSystemItem[0]);
  }

  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @Override
  @Nonnull
  public Language getLanguage() {
    return Language.ANY;
  }

  @Override
  public void checkSetName(final String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be renamed");
  }

  @RequiredWriteAction
  @Override
  public PsiElement setName(@NonNls @Nonnull final String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Frameworks cannot be renamed");
  }

  @Override
  @Nullable
  public PsiFile getContainingFile() {
    return null;
  }

  @Override
  @Nullable
  public TextRange getTextRange() {
    return null;
  }

  @Override
  public int getStartOffsetInParent() {
    return -1;
  }

  @Override
  public int getTextLength() {
    return -1;
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return -1;
  }

  @Override
  @Nullable
  public String getText() {
    return null;
  }

  @Override
  @Nonnull
  public char[] textToCharArray() {
    return ArrayUtil.EMPTY_CHAR_ARRAY; // TODO throw new InsupportedOperationException()
  }

  @Override
  public boolean textMatches(@Nonnull CharSequence text) {
    return false;
  }

  @Override
  public boolean textMatches(@Nonnull PsiElement element) {
    return false;
  }

  @Override
  public PsiElement copy() {
    LOG.error("method not implemented");
    return null;
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }
}
