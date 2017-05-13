package myeslib3.stack1.stack1infra.utils;

import myeslib3.core.data.AggregateRoot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

	public static String aggregateRootId(Class<? extends AggregateRoot> aggregateRootClass) {
		return camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}

	public static String commandId(Class<?> commandClass) {
		return camelCaseToSnakeCase(commandClass.getSimpleName());
	}

	public static String aggrCmdRoot(String prefix,
                                   Class<? extends AggregateRoot> aggregateRootClass, Class<?> commandClass) {
		return camelCaseToSnakeCase(prefix + aggregateRootClass.getSimpleName() + "-" + commandClass.getSimpleName());
	}

	public static String camelCaseToSnakeCase(String start) {
		Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(start);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "_" + m.group());
		}
		m.appendTail(sb);
		return sb.toString().toLowerCase();
	}

	public static String snakeCaseToCamelCase(String start) {
		StringBuffer sb = new StringBuffer();
		for (String s : start.split("_")) {
			sb.append(Character.toUpperCase(s.charAt(0)));
			if (s.length() > 1) {
				sb.append(s.substring(1, s.length()).toLowerCase());
			}
		}
		return sb.toString().toLowerCase();
	}

}
