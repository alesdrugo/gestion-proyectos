package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;  // 👈 Inyectamos el servicio aquí

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path = req.getRequestURI();
        HttpSession session = req.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("username") != null);

        // Rutas públicas que no requieren sesión
        if (path.equals("/login") ||
            path.equals("/register") ||
            path.equals("/error") ||
            path.startsWith("/h2-console") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/") ||
            path.startsWith("/uploads/") ||
            path.equals("/favicon.ico")) {
            return true;
        }

        // Si no está logueado → redirigir a login
        if (!loggedIn) {
            res.sendRedirect("/login");
            return false;
        }

        // 🔒 Comprobación de si el usuario sigue habilitado
        String username = (String) session.getAttribute("username");
        if (!userService.isEnabled(username)) {
            System.out.println("🚫 Usuario deshabilitado: " + username);
            session.invalidate();
            res.sendRedirect("/login");
            return false;
        }

        return true;
    }
}