package com.intellij.openapi.externalSystem.model;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

import java.io.Serializable;

/**
 * The general idea of 'external system' integration is to provide management facilities for the project structure defined in
 * terms over than IntelliJ (e.g. maven, gradle, eclipse etc).
 * <p/>
 * This class serves as an id of a system which defines project structure, i.e. it might be any external system or the ide itself.
 * 
 * @author Denis Zhdanov
 * @since 2/14/12 12:59 PM
 */
public class ProjectSystemId implements Serializable {

  private static final long serialVersionUID = 1L;
  
  @Nonnull
  public static final ProjectSystemId IDE = new ProjectSystemId("IDE");

  @Nonnull
  private final String myId;
  @Nonnull
  private final String myReadableName;

  public ProjectSystemId(@Nonnull String id) {
    this(id, StringUtil.capitalize(id.toLowerCase()));
  }

  public ProjectSystemId(@Nonnull String id, @Nonnull String readableName) {
    myId = id;
    myReadableName = readableName;
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectSystemId owner = (ProjectSystemId)o;

    return myId.equals(owner.myId);
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  @Nonnull
  public String getReadableName() {
    return myReadableName;
  }
  
  @Override
  public String toString() {
    return myId;
  }
}
