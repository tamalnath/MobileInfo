package org.tamal.mobileinfo;

import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Utils {

    private static final String TAG = "Utils";

    private Utils() {
    }

    @SuppressWarnings("unchecked")
    static <T> Map<String, T> findConstants(Class<?> classType, @Nullable Class<T> fieldType, @Nullable String regex) {
        Map<String, T> map = new TreeMap<>();
        Pattern pattern = regex == null ? null : Pattern.compile(regex);
        for (Field field : classType.getDeclaredFields()) {
            boolean isPublic = Modifier.isPublic(field.getModifiers());
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            boolean isFinal = Modifier.isFinal(field.getModifiers());
            if (!isPublic || !isStatic || !isFinal) {
                continue;
            }
            if (fieldType != null && field.getType() != fieldType) {
                continue;
            }
            String name = field.getName();
            if (pattern != null) {
                Matcher matcher = pattern.matcher(name);
                if (!matcher.find()) {
                    continue;
                }
                if (matcher.groupCount() == 1) {
                    name = matcher.group(1);
                }
            }
            try {
                map.put(name, (T) field.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }

    static String findConstant(Class<?> classType, Object value, String regex) {
        Pattern pattern = regex == null ? null : Pattern.compile(regex);
        for (Field field : classType.getDeclaredFields()) {
            boolean isPublic = Modifier.isPublic(field.getModifiers());
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            boolean isFinal = Modifier.isFinal(field.getModifiers());
            if (!isPublic || !isStatic || !isFinal) {
                continue;
            }
            String name = field.getName();
            if (pattern != null) {
                Matcher matcher = pattern.matcher(name);
                if (!matcher.find()) {
                    continue;
                }
                if (matcher.groupCount() == 1) {
                    name = matcher.group(1);
                }
            }
            try {
                if (field.get(null).equals(value)) {
                    return name;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return "";
    }

    static Map<String, Object> findProperties(Object object) {
        return findProperties(object, "^(?:is|get)(.*)$");
    }

    static Map<String, Object> findProperties(Object object, String regex) {
        Map<String, Object> map = new TreeMap<>();
        if (object == null) {
            return map;
        }
        Pattern pattern;
        pattern = Pattern.compile(regex);
        for (Method method : object.getClass().getMethods()) {
            boolean isPublic = Modifier.isPublic(method.getModifiers());
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (!isPublic || isStatic || method.getParameterTypes().length != 0
                    || Object.class == method.getDeclaringClass()) {
                continue;
            }
            String name = method.getName();
            Matcher matcher = pattern.matcher(name);
            if (!matcher.find()) {
                continue;
            }
            if (matcher.groupCount() == 1) {
                name = matcher.group(1);
            }
            try {
                Object value = method.invoke(object);
                map.put(name, value);
            } catch (IllegalAccessException e) {
                Log.d(TAG, "Method: " + method + " Error: " + e.toString() + " Cause: " + e.getCause());
            } catch (InvocationTargetException e) {
                Log.e(TAG, "Method: " + method + " Error: " + e.toString() + " Cause: " + e.getCause());
            }
        }
        return map;
    }

    static Map<String, Object> findFields(Object object) {
        Map<String, Object> map = new TreeMap<>();
        if (object == null) {
            return map;
        }
        for (Field field : object.getClass().getFields()) {
            boolean isPublic = Modifier.isPublic(field.getModifiers());
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            if (!isPublic || isStatic) {
                continue;
            }
            try {
                Object value = field.get(object);
                map.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                Log.d(TAG, "Field: " + field + " Error: " + e.getMessage() + " Cause: " + e.getCause());
            }
        }
        return map;
    }

    static String toString(Object obj) {
        return toString(obj, null, null, null, null);
    }

    static String toString(Object obj, String separator, String start, String end, String keyValSep) {
        if (obj == null) {
            return "null";
        }
        if (separator == null) {
            separator = "\n";
        }
        if (start == null) {
            start = "";
        }
        if (end == null) {
            end = "";
        }
        if (keyValSep == null) {
            keyValSep = ":";
        }
        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                String val = toString(Array.get(obj, i));
                sb.append(separator).append(val);
            }
            if (sb.length() == 0) {
                sb.insert(0, start);
            } else {
                sb.replace(0, separator.length(), start);
            }
            sb.append(end);
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder();
            for (Object item : collection) {
                String val = toString(item);
                sb.append(separator).append(val);
            }
            if (sb.length() == 0) {
                sb.insert(0, start);
            } else {
                sb.replace(0, separator.length(), start);
            }
            sb.append(end);
            return sb.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = toString(entry.getKey());
                String value = toString(entry.getValue());
                sb.append(separator).append(key).append(keyValSep).append(value);
            }
            if (sb.length() == 0) {
                sb.insert(0, start);
            } else {
                sb.replace(0, separator.length(), start);
            }
            sb.append(end);
            return sb.toString();
        }

        try {
            if (obj.getClass().getMethod("toString").getDeclaringClass() == Object.class) {
                return "";
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return String.valueOf(obj);
    }

    static void expand(Map<String, Object> map, String key, Class<?> classType, String regex) {
        Object value = map.get(key);
        if (value == null) {
            return;
        }
        if (value.getClass().isArray()) {
            Map<String, ?> CONST = findConstants(classType, value.getClass().getComponentType(), regex);
            int length = Array.getLength(value);
            String[] array = new String[length];
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                for (Map.Entry<String, ?> entry : CONST.entrySet()) {
                    if (entry.getValue().equals(item)) {
                        array[i] = entry.getKey();
                        break;
                    }
                }
            }
            map.put(key, array);
        } else {
            value = findConstant(classType, value, regex);
            if (value != null) {
                map.put(key, value);
            }
        }
    }
}
