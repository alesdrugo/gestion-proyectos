package es.merida.tfg.gestion_proyectos.controller;

import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

public class Authz {

    public static String username(HttpSession session) {
        Object u = session.getAttribute("username");
        return (u instanceof String) ? (String) u : null;
    }

    public static List<String> roles(HttpSession session) {
        Object r = session.getAttribute("roles");
        if (r == null) return List.of();

        if (r instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        if (r instanceof Set<?> set) {
            return set.stream().map(String::valueOf).collect(Collectors.toList());
        }
        if (r.getClass().isArray()) {
            Object[] arr = (Object[]) r;
            return Arrays.stream(arr).map(String::valueOf).toList();
        }
        if (r instanceof String s) {
            // por si guardas "ROLE_USER,ROLE_MANAGER"
            return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
        }
        return List.of();
    }

    public static boolean hasRole(HttpSession session, String role) {
        return roles(session).stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }

    public static boolean hasAnyRole(HttpSession session, String... roles) {
        for (String role : roles) if (hasRole(session, role)) return true;
        return false;
    }

    public static Long teamId(HttpSession session) {
        Object t = session.getAttribute("teamId");
        if (t == null) return null;
        if (t instanceof Long l) return l;
        if (t instanceof Integer i) return i.longValue();
        if (t instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static boolean isPending(HttpSession session) {
        return teamId(session) == null || roles(session).isEmpty();
    }
}
