/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditReadOnlyListener;
import com.intellij.openapi.editor.ex.LineIterator;
import com.intellij.openapi.editor.ex.RangeMarkerEx;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.reference.SoftReference;
import com.intellij.util.Processor;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.ImmutableCharSequence;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class FrozenDocument implements DocumentEx {
  private final ImmutableCharSequence myText;
  @Nullable
  private volatile LineSet myLineSet;
  private final long myStamp;
  private volatile SoftReference<String> myTextString;

  FrozenDocument(@Nonnull ImmutableCharSequence text, @Nullable LineSet lineSet, long stamp, @Nullable String textString) {
    myText = text;
    myLineSet = lineSet;
    myStamp = stamp;
    myTextString = textString == null ? null : new SoftReference<String>(textString);
  }

  @Nonnull
  private LineSet getLineSet() {
    LineSet lineSet = myLineSet;
    if (lineSet == null) {
      myLineSet = lineSet = LineSet.createLineSet(myText);
    }
    return lineSet;
  }

  public FrozenDocument applyEvent(DocumentEvent event, int newStamp) {
    final int offset = event.getOffset();
    final int oldEnd = offset + event.getOldLength();
    ImmutableCharSequence newText = myText.delete(offset, oldEnd).insert(offset, event.getNewFragment());
    LineSet newLineSet = getLineSet().update(myText, offset, oldEnd, event.getNewFragment(), event.isWholeTextReplaced());
    return new FrozenDocument(newText, newLineSet, newStamp, null);
  }

  @Override
  public void setStripTrailingSpacesEnabled(boolean isEnabled) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public LineIterator createLineIterator() {
    return getLineSet().createIterator();
  }

  @Override
  public void setModificationStamp(long modificationStamp) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceText(@Nonnull CharSequence chars, long newModificationStamp) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void moveText(int srcStart, int srcEnd, int dstOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void suppressGuardedExceptions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unSuppressGuardedExceptions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInEventsHandling() {
    return false;
  }

  @Override
  public void clearLineModificationFlags() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeRangeMarker(@Nonnull RangeMarkerEx rangeMarker) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerRangeMarker(@Nonnull RangeMarkerEx rangeMarker,
                                  int start,
                                  int end,
                                  boolean greedyToLeft,
                                  boolean greedyToRight,
                                  int layer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInBulkUpdate() {
    return false;
  }

  @Override
  public void setInBulkUpdate(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public List<RangeMarker> getGuardedBlocks() {
    return Collections.emptyList();
  }

  @Override
  public boolean processRangeMarkers(@Nonnull Processor<? super RangeMarker> processor) {
    return true;
  }

  @Override
  public boolean processRangeMarkersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeMarker> processor) {
    return true;
  }

  @Nonnull
  @Override
  public String getText() {
    String s = SoftReference.dereference(myTextString);
    if (s == null) {
      myTextString = new SoftReference<String>(s = myText.toString());
    }
    return s;
  }

  @Nonnull
  @Override
  public String getText(@Nonnull TextRange range) {
    return myText.subSequence(range.getStartOffset(), range.getEndOffset()).toString();
  }

  @Nonnull
  @Override
  public CharSequence getCharsSequence() {
    return myText;
  }

  @Nonnull
  @Override
  public CharSequence getImmutableCharSequence() {
    return myText;
  }

  @Nonnull
  @Override
  public char[] getChars() {
    return CharArrayUtil.fromSequence(myText);
  }

  @Override
  public int getTextLength() {
    return myText.length();
  }

  @Override
  public int getLineCount() {
    return getLineSet().getLineCount();
  }

  @Override
  public int getLineNumber(int offset) {
    return getLineSet().findLineIndex(offset);
  }

  @Override
  public int getLineStartOffset(int line) {
    if (line == 0) return 0; // otherwise it crashed for zero-length document
    return getLineSet().getLineStart(line);
  }

  @Override
  public int getLineEndOffset(int line) {
    if (getTextLength() == 0 && line == 0) return 0;
    int result = getLineSet().getLineEnd(line) - getLineSeparatorLength(line);
    assert result >= 0;
    return result;
  }

  @Override
  public void insertString(int offset, @Nonnull CharSequence s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteString(int startOffset, int endOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceString(int startOffset, int endOffset, @Nonnull CharSequence s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public long getModificationStamp() {
    return myStamp;
  }

  @Override
  public void fireReadOnlyModificationAttempt() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeDocumentListener(@Nonnull DocumentListener listener) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RangeMarker createRangeMarker(int startOffset, int endOffset) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RangeMarker createRangeMarker(int startOffset, int endOffset, boolean surviveOnExternalChange) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReadOnly(boolean isReadOnly) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RangeMarker createGuardedBlock(int startOffset, int endOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeGuardedBlock(@Nonnull RangeMarker block) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public RangeMarker getOffsetGuard(int offset) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public RangeMarker getRangeGuard(int start, int end) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void startGuardedBlockChecking() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stopGuardedBlockChecking() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCyclicBufferSize(int bufferSize) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setText(@Nonnull CharSequence text) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RangeMarker createRangeMarker(@Nonnull TextRange textRange) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLineSeparatorLength(int line) {
    return getLineSet().getSeparatorLength(line);
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getModificationSequence() {
    return 0;
  }
}
