package methodx;

class Exceptions {

    static class ParseArgumentException extends Exception {

        ParseArgumentException(String argName, Class<?> argType, String argValue, Throwable cause) {
            super("failed to parse argument [" + argName + "] of type [" + argType.getName() + "] from value: [" + argValue + "]", cause);
        }
    }
}
