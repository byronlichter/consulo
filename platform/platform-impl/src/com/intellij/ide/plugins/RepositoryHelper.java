/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.plugins;

import com.google.gson.Gson;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.net.HttpConfigurable;
import consulo.ide.plugins.PluginJsonNode;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author stathik
 * @since Mar 28, 2003
 */
public class RepositoryHelper {
  @NotNull
  public static String buildUrlForList(@NotNull UpdateChannel channel) {
    ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();

    return appInfo.getPluginsListUrl() + "?platformVersion=" + appInfo.getBuild().asString() + "&channel=" + channel;
  }

  @NotNull
  public static String buildUrlForDownload(@NotNull UpdateChannel channel, @NotNull String pluginId) {
    ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();

    String id = PermanentInstallationID.get();

    return appInfo.getPluginsDownloadUrl() + "?platformVersion=" + appInfo.getBuild().asString() + "&channel=" + channel + "&pluginId=" + pluginId + "&id=" + id;
  }

  @NotNull
  @Deprecated
  public static List<IdeaPluginDescriptor> loadPluginsFromRepository(@Nullable ProgressIndicator indicator) throws Exception {
    return loadPluginsFromRepository(indicator, UpdateSettings.getInstance().getChannel());
  }

  @NotNull
  public static List<IdeaPluginDescriptor> loadPluginsFromRepository(@Nullable ProgressIndicator indicator, @NotNull UpdateChannel channel) throws Exception {
    ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();

    String url = buildUrlForList(channel);

    if (indicator != null) {
      indicator.setText2(IdeBundle.message("progress.connecting.to.plugin.manager", appInfo.getPluginManagerUrl()));
    }

    HttpURLConnection connection = HttpConfigurable.getInstance().openHttpConnection(url);
    connection.setRequestProperty("Accept-Encoding", "gzip");

    if (indicator != null) {
      indicator.setText2(IdeBundle.message("progress.waiting.for.reply.from.plugin.manager", appInfo.getPluginManagerUrl()));
    }

    connection.connect();
    try {
      if (indicator != null) {
        indicator.checkCanceled();
      }

      String encoding = connection.getContentEncoding();
      InputStream is = connection.getInputStream();
      try {
        if ("gzip".equalsIgnoreCase(encoding)) {
          is = new GZIPInputStream(is);
        }

        if (indicator != null) {
          indicator.setText2(IdeBundle.message("progress.downloading.list.of.plugins"));
        }

        return readPluginsStream(is, indicator);
      }
      finally {
        is.close();
      }
    }
    finally {
      connection.disconnect();
    }
  }

  private static List<IdeaPluginDescriptor> readPluginsStream(InputStream is, ProgressIndicator indicator) throws Exception {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      byte[] buffer = new byte[1024];
      int size;
      while ((size = is.read(buffer)) > 0) {
        os.write(buffer, 0, size);
        if (indicator != null) {
          indicator.checkCanceled();
        }
      }
    }
    finally {
      os.close();
    }

    PluginJsonNode[] nodes =
            new Gson().fromJson(new InputStreamReader(new ByteArrayInputStream(os.toByteArray()), StandardCharsets.UTF_8), PluginJsonNode[].class);

    List<IdeaPluginDescriptor> pluginDescriptors = new ArrayList<>(nodes.length);
    for (PluginJsonNode jsonPlugin : nodes) {
      pluginDescriptors.add(new PluginNode(jsonPlugin));
    }
    return pluginDescriptors;
  }

  @Deprecated
  public static List<IdeaPluginDescriptor> loadPluginsFromDescription(InputStream is, ProgressIndicator indicator) throws Exception {
    return Collections.emptyList();
  }
}
