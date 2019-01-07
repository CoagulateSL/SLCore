package net.coagulate.SL.Pages.HTML;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Iain Price
 */
public class TRColourGenerator extends TRGenerator {

    private final String columnname;
    private final String defaultcolour;
    public TRColourGenerator(String columnname,String defaultcolour) { this.columnname=columnname; this.defaultcolour=defaultcolour; }

    Map<String,String> valuemap=new HashMap<>();
    public TRColourGenerator map(String value,String colour) { valuemap.put(value,colour); return this; }
    @Override
    public String render(State st, Map<String, String> row) {
        return "<tr bgcolor=\"#"+getColour(st,row)+"\">";
    }
    private String getColour(State st,Map<String,String> row) {
        if (!row.containsKey(columnname)) { return defaultcolour; }
        String value=row.get(columnname);
        if (!valuemap.containsKey(value)) { return defaultcolour; }
        return valuemap.get(value);
    }
}
