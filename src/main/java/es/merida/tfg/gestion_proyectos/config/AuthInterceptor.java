package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path = req.getRequestURI();

        if (isPublicPath(path)) return true;

        HttpSession session = req.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : null;

        if (username == null) {
            res.sendRedirect("/login");
            return false;
        }

        // Si está deshabilitado, fuera
        if (!userService.isEnabled(username)) {
            session.invalidate();
            res.sendRedirect("/login");
            return false;
        }

        // Roles y teamId desde sesión
        @SuppressWarnings("unchecked")
        List<String> roles = (session.getAttribute("roles") instanceof List)
                ? (List<String>) session.getAttribute("roles")
                : List.of();

        Object t = session.getAttribute("teamId");
        Long teamId = (t instanceof Long) ? (Long) t : null;

        boolean isAdmin = roles.stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r));
        boolean isPending = (teamId == null) || roles.isEmpty();

        // Si es PENDING y no es admin, solo dejamos pending/logout
        if (isPending && !isAdmin) {
            if (path.equals("/pending") || path.equals("/logout") || path.equals("/login")) {
                return true;
            }
            res.sendRedirect("/pending");
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/login")
                || path.equals("/register")
                || path.equals("/pending")          
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.startsWith("/h2-console")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/uploads/");
    }
}
