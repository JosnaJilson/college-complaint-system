package com.cms.web;

import com.cms.enums.ComplaintStatus;
import com.cms.enums.Priority;
import com.cms.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.cms.service.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private final CategoryService categoryService = new CategoryService();
    private final AuthService authService = new AuthService(categoryService);
    private final ComplaintService complaintService = new ComplaintService();

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/style.css", this::styleCss);
        server.createContext("/login", this::login);
        server.createContext("/signup", this::signup);
        server.createContext("/logout", this::logout);
        server.createContext("/dashboard", this::dashboard);
        server.createContext("/file", this::fileComplaint);
        server.createContext("/admin", this::admin);
        server.createContext("/escalate", this::escalateSweep);
        server.createContext("/", this::index);
        server.setExecutor(null);
        server.start();
        System.out.println("Complaint Management System running at http://localhost:" + port);
        System.out.println("Sample admin logins (password 'admin123'): hostel.warden@college.edu, cse.ai@college.edu, canteen.admin@college.edu");
        System.out.println("Student signup requires an email ending in " + AuthService.ALLOWED_STUDENT_DOMAIN);
    }

    // ---------- routing helpers ----------

    private void index(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (u instanceof Admin) redirect(ex, "/admin");
        else if (u instanceof Student) redirect(ex, "/dashboard");
        else redirect(ex, "/login");
    }

    private User currentUser(HttpExchange ex) {
        String token = getCookie(ex, "session");
        return authService.getUserForToken(token);
    }

    private String getCookie(HttpExchange ex, String name) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String header : cookies) {
            for (String part : header.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) return kv[1];
            }
        }
        return null;
    }

    private void setSessionCookie(HttpExchange ex, String token) {
        ex.getResponseHeaders().add("Set-Cookie", "session=" + token + "; Path=/; HttpOnly");
    }

    private void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    private void html(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, val);
        }
        return map;
    }

    // ---------- pages ----------

    private void styleCss(HttpExchange ex) throws IOException {
        String css = "body{font-family:Segoe UI,Arial,sans-serif;background:#f4f6f8;margin:0;color:#1f2937}"
            + ".wrap{max-width:900px;margin:30px auto;padding:0 20px}"
            + ".card{background:#fff;border-radius:10px;padding:24px;box-shadow:0 1px 4px rgba(0,0,0,.08);margin-bottom:20px}"
            + "h1{color:#111827} h2{color:#1f2937;margin-top:0}"
            + "nav{background:#111827;padding:14px 20px} nav a{color:#fff;margin-right:18px;text-decoration:none;font-weight:600}"
            + "input,select,textarea,button{font-size:15px;padding:8px;margin:6px 0;width:100%;box-sizing:border-box;border:1px solid #ccc;border-radius:6px}"
            + "button{background:#2563eb;color:#fff;border:none;cursor:pointer;font-weight:600}"
            + "button:hover{background:#1d4ed8}"
            + "table{width:100%;border-collapse:collapse} td,th{padding:10px;border-bottom:1px solid #eee;text-align:left;font-size:14px}"
            + ".badge{padding:3px 10px;border-radius:12px;font-size:12px;font-weight:700;color:#fff}"
            + ".OPEN{background:#f59e0b}.IN_PROGRESS{background:#3b82f6}.ESCALATED{background:#dc2626}"
            + ".RESOLVED{background:#16a34a}.CLOSED{background:#6b7280}.REOPENED{background:#a855f7}"
            + ".pr-LOW{color:#6b7280}.pr-MEDIUM{color:#2563eb}.pr-HIGH{color:#f59e0b;font-weight:700}.pr-CRITICAL{color:#dc2626;font-weight:800}"
            + ".error{color:#dc2626;font-weight:600}";
        ex.getResponseHeaders().add("Content-Type", "text/css");
        byte[] b = css.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private String layout(String title, String content) {
        return "<!doctype html><html><head><meta charset='utf-8'><title>" + title
            + "</title><link rel='stylesheet' href='/style.css'></head><body>"
            + "<nav><a href='/'>CMS</a><a href='/dashboard'>Student</a><a href='/admin'>Admin</a><a href='/logout'>Logout</a></nav>"
            + "<div class='wrap'>" + content + "</div></body></html>";
    }

    private void login(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> f = readForm(ex);
            try {
                String token = authService.login(f.get("email"), f.get("password"));
                setSessionCookie(ex, token);
                redirect(ex, "/");
                return;
            } catch (IllegalArgumentException e) {
                html(ex, layout("Login", loginForm(e.getMessage())));
                return;
            }
        }
        html(ex, layout("Login", loginForm(null)));
    }

    private String loginForm(String error) {
        return "<div class='card'><h1>Complaint Management System</h1>"
            + "<h2>Login</h2>"
            + (error != null ? "<p class='error'>" + error + "</p>" : "")
            + "<form method='post' action='/login'>"
            + "<input name='email' placeholder='Email' required>"
            + "<input name='password' type='password' placeholder='Password' required>"
            + "<button type='submit'>Login</button></form>"
            + "<p>New student? <a href='/signup'>Sign up</a> with your college email.</p></div>";
    }

    private void signup(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> f = readForm(ex);
            try {
                String token = authService.signupStudent(f.get("name"), f.get("email"), f.get("password"), f.get("department"));
                setSessionCookie(ex, token);
                redirect(ex, "/dashboard");
                return;
            } catch (IllegalArgumentException e) {
                html(ex, layout("Sign up", signupForm(e.getMessage())));
                return;
            }
        }
        html(ex, layout("Sign up", signupForm(null)));
    }

    private String signupForm(String error) {
        return "<div class='card'><h1>Student Sign Up</h1>"
            + (error != null ? "<p class='error'>" + error + "</p>" : "")
            + "<form method='post' action='/signup'>"
            + "<input name='name' placeholder='Full name' required>"
            + "<input name='email' placeholder='you" + AuthService.ALLOWED_STUDENT_DOMAIN + "' required>"
            + "<input name='password' type='password' placeholder='Password' required>"
            + "<input name='department' placeholder='Department (e.g. CSE)' required>"
            + "<button type='submit'>Create account</button></form>"
            + "<p>Already have an account? <a href='/login'>Login</a></p></div>";
    }

    private void logout(HttpExchange ex) throws IOException {
        authService.logout(getCookie(ex, "session"));
        redirect(ex, "/login");
    }

    private void dashboard(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!(u instanceof Student)) { redirect(ex, "/login"); return; }
        Student s = (Student) u;
        List<Complaint> mine = complaintService.forStudent(s);
        StringBuilder rows = new StringBuilder();
        for (Complaint c : mine) {
            rows.append("<tr><td>#").append(c.getId()).append("</td><td>").append(escape(c.getTitle()))
                .append("</td><td>").append(escape(c.getCategory().getPath())).append("</td>")
                .append("<td class='pr-").append(c.getPriority()).append("'>").append(c.getPriority()).append("</td>")
                .append("<td><span class='badge ").append(c.getStatus()).append("'>").append(c.getStatus()).append("</span></td>")
                .append("<td>").append(c.getDateFiledFormatted()).append("</td></tr>");
        }
        String content = "<div class='card'><h1>Welcome, " + escape(s.getName()) + "</h1>"
            + "<p>" + s.getDepartment() + " &middot; " + s.getEmail() + "</p>"
            + "<a href='/file'><button style='width:auto;padding:10px 20px'>File a new complaint</button></a></div>"
            + "<div class='card'><h2>Your complaints (sorted by priority)</h2>"
            + "<table><tr><th>ID</th><th>Title</th><th>Category</th><th>Priority</th><th>Status</th><th>Filed</th></tr>"
            + (mine.isEmpty() ? "<tr><td colspan='6'>No complaints filed yet.</td></tr>" : rows)
            + "</table></div>";
        html(ex, layout("Dashboard", content));
    }

    private void fileComplaint(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!(u instanceof Student)) { redirect(ex, "/login"); return; }
        Student s = (Student) u;
        String error = null;
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> f = readForm(ex);
            try {
                int catId = Integer.parseInt(f.get("categoryId"));
                Category cat = categoryService.findById(catId);
                Priority pr = Priority.valueOf(f.get("priority"));
                complaintService.file(s, cat, f.get("title"), f.get("description"), pr);
                redirect(ex, "/dashboard");
                return;
            } catch (Exception e) {
                error = "Could not file complaint: " + e.getMessage();
            }
        }
        html(ex, layout("File Complaint", fileForm(error)));
    }

    private String fileForm(String error) {
        String catJson = categoriesAsJson();
        return "<div class='card'><h1>File a Complaint</h1>"
            + (error != null ? "<p class='error'>" + error + "</p>" : "")
            + "<form method='post' action='/file' id='cform'>"
            + "<label>Category</label><div id='levels'></div>"
            + "<input type='hidden' name='categoryId' id='categoryId' required>"
            + "<label>Priority</label><select name='priority'>"
            + "<option value='LOW'>Low</option><option value='MEDIUM' selected>Medium</option>"
            + "<option value='HIGH'>High</option><option value='CRITICAL'>Critical</option></select>"
            + "<label>Title</label><input name='title' required>"
            + "<label>Description</label><textarea name='description' rows='4' required></textarea>"
            + "<button type='submit' id='submitBtn' disabled>File complaint</button>"
            + "</form></div>"
            + "<script>"
            + "const CATS=" + catJson + ";"
            + "function childrenOf(pid){return CATS.filter(c=>c.parentId===pid);}"
            + "function renderLevel(pid, container){"
            + "  const kids = childrenOf(pid);"
            + "  if(kids.length===0){document.getElementById('categoryId').value=pid;document.getElementById('submitBtn').disabled=false;return;}"
            + "  document.getElementById('submitBtn').disabled=true;"
            + "  const sel=document.createElement('select');"
            + "  sel.innerHTML=\"<option value=''>-- select --</option>\" + kids.map(k=>`<option value='${k.id}'>${k.name}</option>`).join('');"
            + "  sel.onchange=function(){"
            + "    let next=container.nextSibling; while(next){let n=next.nextSibling; next.remove(); next=n;}"
            + "    if(this.value) renderLevel(parseInt(this.value), sel);"
            + "  };"
            + "  container.parentNode.insertBefore(sel, container.nextSibling);"
            + "  container = sel;"
            + "}"
            + "const rootChildren = CATS.filter(c=>c.parentId===null);"
            + "const startSel=document.createElement('select');"
            + "startSel.innerHTML=\"<option value=''>-- select --</option>\" + rootChildren.map(k=>`<option value='${k.id}'>${k.name}</option>`).join('');"
            + "startSel.onchange=function(){"
            + "  let next=startSel.nextSibling; while(next){let n=next.nextSibling; next.remove(); next=n;}"
            + "  document.getElementById('submitBtn').disabled=true;"
            + "  if(this.value) renderLevel(parseInt(this.value), startSel);"
            + "};"
            + "document.getElementById('levels').appendChild(startSel);"
            + "</script>";
    }

    private String categoriesAsJson() {
        List<Category> all = categoryService.getAll();
        List<String> parts = new ArrayList<>();
        for (Category c : all) {
            if (c.getParent() == null) continue; // skip synthetic root
            Integer pid = c.getParent().getParent() == null ? null : c.getParent().getId();
            parts.add("{\"id\":" + c.getId() + ",\"name\":\"" + escapeJs(c.getName()) + "\",\"parentId\":" + (pid == null ? "null" : pid) + "}");
        }
        return "[" + String.join(",", parts) + "]";
    }

    private void admin(HttpExchange ex) throws IOException {
        User u = currentUser(ex);
        if (!(u instanceof Admin)) { redirect(ex, "/login"); return; }
        Admin admin = (Admin) u;

        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> f = readForm(ex);
            try {
                int id = Integer.parseInt(f.get("complaintId"));
                String action = f.get("action");
                if ("start".equals(action)) complaintService.updateStatus(id, ComplaintStatus.IN_PROGRESS, "Admin started work");
                else if ("resolve".equals(action)) complaintService.resolve(id, f.getOrDefault("notes", ""));
                else if ("close".equals(action)) complaintService.updateStatus(id, ComplaintStatus.CLOSED, "Closed by admin");
            } catch (Exception ignored) { /* fall through to re-render */ }
            redirect(ex, "/admin");
            return;
        }

        List<Complaint> mine = complaintService.forAdmin(admin);
        StringBuilder rows = new StringBuilder();
        for (Complaint c : mine) {
            rows.append("<tr><td>#").append(c.getId()).append("</td><td>").append(escape(c.getTitle()))
                .append("<br><small>").append(escape(c.getDescription())).append("</small></td>")
                .append("<td>").append(escape(c.getFiledBy().getName())).append("</td>")
                .append("<td class='pr-").append(c.getPriority()).append("'>").append(c.getPriority())
                .append(c.getEscalationLevel() > 0 ? " (esc. x" + c.getEscalationLevel() + ")" : "").append("</td>")
                .append("<td><span class='badge ").append(c.getStatus()).append("'>").append(c.getStatus()).append("</span></td>")
                .append("<td>")
                .append(actionForm(c.getId(), "start", "Start"))
                .append(actionForm(c.getId(), "resolve", "Resolve"))
                .append(actionForm(c.getId(), "close", "Close"))
                .append("</td></tr>");
        }
        String content = "<div class='card'><h1>" + escape(admin.getTitle()) + "</h1><p>" + admin.getEmail() + "</p>"
            + "<form method='get' action='/escalate' style='display:inline'><button style='width:auto;padding:8px 16px'>Run escalation sweep</button></form></div>"
            + "<div class='card'><h2>Assigned complaints (priority order)</h2>"
            + "<table><tr><th>ID</th><th>Complaint</th><th>Student</th><th>Priority</th><th>Status</th><th>Actions</th></tr>"
            + (mine.isEmpty() ? "<tr><td colspan='6'>No complaints assigned to you.</td></tr>" : rows)
            + "</table></div>";
        html(ex, layout("Admin", content));
    }

    private String actionForm(int id, String action, String label) {
        return "<form method='post' action='/admin' style='display:inline;width:auto'>"
            + "<input type='hidden' name='complaintId' value='" + id + "'>"
            + "<input type='hidden' name='action' value='" + action + "'>"
            + "<button style='width:auto;padding:4px 10px;margin:2px;font-size:12px' type='submit'>" + label + "</button></form>";
    }

    private void escalateSweep(HttpExchange ex) throws IOException {
        complaintService.runEscalationSweep();
        redirect(ex, "/admin");
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
