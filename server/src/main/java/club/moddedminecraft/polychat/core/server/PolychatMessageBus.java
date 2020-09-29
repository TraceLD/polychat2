package club.moddedminecraft.polychat.core.server;

import club.moddedminecraft.polychat.core.networklibrary.ConnectedClient;
import club.moddedminecraft.polychat.core.networklibrary.Message;

import com.google.protobuf.Any;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;

public final class PolychatMessageBus {
    private final static Logger logger = LoggerFactory.getLogger(PolychatMessageBus.class);
    private final ArrayList<Object> eventHandlers = new ArrayList<>();

    public void addEventHandlers(Object... eventHandlers) {
        this.eventHandlers.addAll(Arrays.asList(eventHandlers));
    }

    public void removeEventHandler(Object eventHandler) {
        eventHandlers.remove(eventHandler);
    }

    public void handlePolychatMessage(Message message) {
        try {
            Any packedProtoMessage = Any.parseFrom(message.getData());
            for (Object handler : eventHandlers) {
                Class<?> clazz = handler.getClass();
                for (Method method : clazz.getMethods()) {
                    if (isAcceptableEventHandler(method)) {
                        Parameter parameter = method.getParameters()[0]; //isAcceptableEventHandler verifies that there is exactly 1 argument
                        Class<?> parameterType = parameter.getType();
                        @SuppressWarnings("unchecked")
                        Class<? extends com.google.protobuf.Message> castedParameterType = (Class<? extends com.google.protobuf.Message>) parameterType; //this class is checked in isAcceptableEventHandler
                        if (packedProtoMessage.is(castedParameterType)) {
                            method.invoke(handler, packedProtoMessage.unpack(castedParameterType), message.getFrom());
                        }
                    }
                }
            }
        } catch (Throwable t) { //catch errors from event handlers + the parseFrom error
            logger.error("Failed to parse/unpack/handle message.", t);
        }
    }

    private boolean isAcceptableEventHandler(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }

        if (method.getParameterCount() != 2) {
            return false;
        }

        if(method.getParameters()[1].getType() != ConnectedClient.class){
            return false;
        }

        if (!com.google.protobuf.Message.class.isAssignableFrom(method.getParameters()[0].getType())) {
            return false;
        }

        if (method.getAnnotation(EventHandler.class) == null) {
            return false;
        }

        return true;
    }

}