package net.coagulate.SL;

import java.util.*;

public class ChangeLogging {
    public enum CHANGETYPE {
        Add,
        Delete,
        Fix,
        Change
    }

    public static String asHtml() {
        StringBuilder s=new StringBuilder();
        s.append("<table><tr><th>Date</th><th>Application</th><th></th><th>Component</th><th>Change</th></tr>");
        for (Map<String, Map<String, Set<Change>>> byDate:changes.values()) {
            for (Map<String, Set<Change>> byDateApp:byDate.values()) {
                for (Set<Change> byDateAppComponent:byDateApp.values()) {
                    for (Change c:byDateAppComponent) {
                        s.append("<tr><td>");
                        s.append(c.date().replaceAll("-","&#8209;"));
                        s.append("</td><td>");
                        s.append(c.application());
                        s.append("</td><td><img src=\"/resources/");
                        s.append(c.type());
                        s.append(".png\" height=16px></td><td>");
                        s.append(c.component());
                        s.append("</td><td>");
                        s.append(c.message());
                        s.append("</td></tr>");
                    }
                }
            }
        }
        s.append("</table>");
        return s.toString();
    }
    public static String asHtml(String application) {
        StringBuilder s=new StringBuilder();
        s.append("<table><tr><th>Date</th><th></th><th>Component</th><th>Change</th></tr>");
        for (Map<String, Map<String, Set<Change>>> byDate:changes.values()) {
            for (Map<String, Set<Change>> byDateApp:byDate.values()) {
                for (Set<Change> byDateAppComponent:byDateApp.values()) {
                    for (Change c:byDateAppComponent) {
                        if (c.application().equalsIgnoreCase(application)) {
                            s.append("<tr><td>");
                            s.append(c.date().replaceAll("-", "&#8209;"));
                            s.append("</td><td><img src=\"/resources/");
                            s.append(c.type());
                            s.append(".png\" height=16px></td><td>");
                            s.append(c.component());
                            s.append("</td><td>");
                            s.append(c.message());
                            s.append("</td></tr>");
                        }
                    }
                }
            }
        }
        s.append("</table>");
        return s.toString();
    }
    public record Change(String date, String application, String component, CHANGETYPE type, String message){}

    public static final Map<String,Map<String,Map<String,Set<Change>>>> changes=new TreeMap<>(Comparator.reverseOrder());

    public static void add(Change c) {
        if (!changes.containsKey(c.date())) { changes.put(c.date(),new TreeMap<>()); }
        Map<String, Map<String, Set<Change>>> byDate = changes.get(c.date());
        if (!byDate.containsKey(c.application())) { byDate.put(c.application(),new TreeMap<>()); }
        Map<String, Set<Change>> byDateApp = byDate.get(c.application());
        if (!byDateApp.containsKey(c.component())) { byDateApp.put(c.component(),new HashSet<>()); }
        byDateApp.get(c.component()).add(c);
    }

}
