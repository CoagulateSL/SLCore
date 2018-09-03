package net.coagulate.SL.Data;

/**  Represents a row in a set of database results.
 *
 * @author Iain Price <gphud@predestined.net>
 */
import java.sql.*;
import java.util.*;
import net.coagulate.SL.DBException;
import net.coagulate.SL.SystemException;

public class Row
{
        private final Map<String,String> row=new TreeMap<>();

        /** Construct the frow from a resultset.
         * 
         * @param rs Resultset to read the row from 
         */
        public Row(ResultSet rs) throws DBException
        {
            try {
                ResultSetMetaData rsmd=rs.getMetaData();
                for (int i=1;i<=rsmd.getColumnCount();i++) {
                    row.put(rsmd.getColumnName(i),rs.getString(i));
                }
            } catch (SQLException ex) {
                throw new DBException("Exception unpacking result set",ex);
            }
        }
        public String getString(String s) { return row.get(s); }
        public Integer getInt(String s) { String result=getString(s); if (result==null) { return null; } return Integer.parseInt(result); }
        public float getFloat(String s) { return Float.parseFloat(getString(s)); }
        public String getString() {
                if (row.size()!=1) { throw new SystemException("Column count !=1 - "+row.size()); }
                for (String s:row.keySet()) { return row.get(s); }
                return null;
        }
        public Integer getInt() { if (getString()==null) { return null; } return Integer.parseInt(getString()); }
        public float getFloat() { return Float.parseFloat(getString()); }
        public Set<String> keySet() { return row.keySet(); }
        @Override
        public String toString() {
            String output="[";
            for (String k:keySet()) { 
                if (!output.equals("[")) { output=output+", "; }
                output=output+k+"="+getString(k);
            }
            output+="]";
            return output;
        }
}