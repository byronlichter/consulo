package org.jetbrains.io;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ThrowableNotNullFunction;
import com.intellij.util.io.NettyKt;
import com.intellij.util.ui.UIUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.Imaging;
import org.jetbrains.ide.HttpRequestHandler;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@ChannelHandler.Sharable
final class DelegatingHttpRequestHandler extends DelegatingHttpRequestHandlerBase {
  private static final AttributeKey<HttpRequestHandler> PREV_HANDLER = AttributeKey.valueOf("DelegatingHttpRequestHandler.handler");

  @Override
  protected boolean process(ChannelHandlerContext context, FullHttpRequest request, QueryStringDecoder urlDecoder) throws Exception {
    ThrowableNotNullFunction<HttpRequestHandler, Boolean, IOException> checkAndProcess = httpRequestHandler -> {
      return httpRequestHandler.isSupported(request) &&
             !NettyKt.isWriteFromBrowserWithoutOrigin(request) &&
             httpRequestHandler.isAccessible(request) &&
             httpRequestHandler.process(urlDecoder, request, context);
    };


    Attribute<HttpRequestHandler> prevHandlerAttribute = context.channel().attr(PREV_HANDLER);
    HttpRequestHandler connectedHandler = prevHandlerAttribute.get();
    if (connectedHandler != null) {
      if (checkAndProcess.fun(connectedHandler)) {
        return true;
      }
      // prev cached connectedHandler is not suitable for this request, so, let's find it again
      prevHandlerAttribute.set(null);
    }

    for (HttpRequestHandler handler : HttpRequestHandler.EP_NAME.getExtensions()) {
      try {
        if (checkAndProcess.fun(handler)) {
          prevHandlerAttribute.set(handler);
          return true;
        }
      }
      catch (Throwable e) {
        Logger.getInstance(BuiltInServer.class).error(e);
      }
    }

    if (urlDecoder.path().equals("/favicon.ico")) {
      Icon icon = Application.get().getIcon();
      BufferedImage image = UIUtil.createImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      icon.paintIcon(null, image.getGraphics(), 0, 0);
      byte[] icoBytes = Imaging.writeImageToBytes(image, ImageFormats.ICO, null);

      HttpResponse response = Responses.response(FileResponses.getContentType(urlDecoder.path()), Unpooled.wrappedBuffer(icoBytes));
      response = Responses.addNoCache(response);
      Responses.send(response, context.channel(), request);
      return true;
    }

    return false;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    try {
      ctx.channel().attr(PREV_HANDLER).set(null);
    }
    finally {
      super.exceptionCaught(ctx, cause);
    }
  }
}
