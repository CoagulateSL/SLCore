package net.coagulate.SL.Pages.HTML;

import java.util.ArrayList;
import java.util.List;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;

/**
 *
 * @author Iain Price
 */
public class SQLTable extends Table {

    private final String sql;
    private final Object[] params;
    private final DBConnection db;
    private final List<Column> columns;
    public SQLTable(DBConnection db,String sql,Object... params) {
        this.db=db;
        this.sql=sql;
        this.params=params;
        columns=new ArrayList<>();
    }
    public SQLTable column(String header,String columnname) {
        columns.add(new Column(header,columnname));
        header(header);
        return this;
    }
    public String contentRows(State st) {
        Results results = db.dq(sql,params);
        String r="";
        for (ResultsRow row:results) {
            r+="<tr>";
            for (Column column:columns) {
                r+="<td>";
                r+=row.getString(column.columnname);
                r+="</td>";
            }
            r+="</tr>";
        }
        return r;
    }
    
    class Column {

        final String columnname;
        final String header;
        Column(String header,String columnname) {
            this.header=header;
            this.columnname=columnname;
        }
    }
    
}
