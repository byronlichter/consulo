package com.intellij.remoteServer.impl.configuration;

import com.intellij.openapi.components.*;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServerListener;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@State(name = "RemoteServers", storages = {
  @Storage(file = StoragePathMacros.APP_CONFIG + "/remote-servers.xml")
})
public class RemoteServersManagerImpl extends RemoteServersManager implements PersistentStateComponent<RemoteServersManagerState> {
  public static final SkipDefaultValuesSerializationFilters SERIALIZATION_FILTERS = new SkipDefaultValuesSerializationFilters();
  private List<RemoteServer<?>> myServers = new ArrayList<RemoteServer<?>>();
  private List<RemoteServerState> myUnknownServers = new ArrayList<RemoteServerState>();
  private final MessageBus myMessageBus;

  public RemoteServersManagerImpl(MessageBus messageBus) {
    myMessageBus = messageBus;
  }

  @Override
  public List<RemoteServer<?>> getServers() {
    return Collections.unmodifiableList(myServers);
  }

  @Override
  public <C extends ServerConfiguration> List<RemoteServer<C>> getServers(@Nonnull ServerType<C> type) {
    List<RemoteServer<C>> servers = new ArrayList<RemoteServer<C>>();
    for (RemoteServer<?> server : myServers) {
      if (server.getType().equals(type)) {
        servers.add((RemoteServer<C>)server);
      }
    }
    return servers;
  }

  @javax.annotation.Nullable
  @Override
  public <C extends ServerConfiguration> RemoteServer<C> findByName(@Nonnull String name, @Nonnull ServerType<C> type) {
    for (RemoteServer<?> server : myServers) {
      if (server.getType().equals(type) && server.getName().equals(name)) {
        return (RemoteServer<C>)server;
      }
    }
    return null;
  }

  @Override
  public <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type, @Nonnull String name) {
    return new RemoteServerImpl<C>(name, type, type.createDefaultConfiguration());
  }

  @Override
  public void addServer(RemoteServer<?> server) {
    myServers.add(server);
    myMessageBus.syncPublisher(RemoteServerListener.TOPIC).serverAdded(server);
  }

  @Override
  public void removeServer(RemoteServer<?> server) {
    myServers.remove(server);
    myMessageBus.syncPublisher(RemoteServerListener.TOPIC).serverRemoved(server);
  }

  @javax.annotation.Nullable
  @Override
  public RemoteServersManagerState getState() {
    RemoteServersManagerState state = new RemoteServersManagerState();
    for (RemoteServer<?> server : myServers) {
      RemoteServerState serverState = new RemoteServerState();
      serverState.myName = server.getName();
      serverState.myTypeId = server.getType().getId();
      serverState.myConfiguration = XmlSerializer.serialize(server.getConfiguration().getSerializer().getState(), SERIALIZATION_FILTERS);
      state.myServers.add(serverState);
    }
    state.myServers.addAll(myUnknownServers);
    return state;
  }

  @Override
  public void loadState(RemoteServersManagerState state) {
    myUnknownServers.clear();
    myServers.clear();
    for (RemoteServerState server : state.myServers) {
      ServerType<?> type = findServerType(server.myTypeId);
      if (type == null) {
        myUnknownServers.add(server);
      }
      else {
        myServers.add(createConfiguration(type, server));
      } 
    }
  }

  private static <C extends ServerConfiguration> RemoteServerImpl<C> createConfiguration(ServerType<C> type, RemoteServerState server) {
    C configuration = type.createDefaultConfiguration();
    PersistentStateComponent<?> serializer = configuration.getSerializer();
    ComponentSerializationUtil.loadComponentState(serializer, server.myConfiguration);
    return new RemoteServerImpl<C>(server.myName, type, configuration);
  }

  @javax.annotation.Nullable
  private static ServerType<?> findServerType(@Nonnull String typeId) {
    for (ServerType serverType : ServerType.EP_NAME.getExtensions()) {
      if (serverType.getId().equals(typeId)) {
        return serverType;
      }
    }
    return null;
  }
}
