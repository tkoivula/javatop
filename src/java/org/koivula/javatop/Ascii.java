package org.koivula.javatop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Collections;

public class Ascii {

public static final String ANSI_RESET = "\u001B[0m";

public static final String ANSI_BLACK = "\u001B[30m";
public static final String ANSI_RED = "\u001B[31m";
public static final String ANSI_GREEN = "\u001B[32m";
public static final String ANSI_YELLOW = "\u001B[33m";
public static final String ANSI_BLUE = "\u001B[34m";
public static final String ANSI_PURPLE = "\u001B[35m";
public static final String ANSI_CYAN = "\u001B[36m";
public static final String ANSI_WHITE = "\u001B[37m";

public static final String ANSI_BG_BLACK = "\u001B[40m";
public static final String ANSI_BG_RED = "\u001B[41m";
public static final String ANSI_BG_GREEN = "\u001B[42m";
public static final String ANSI_BG_YELLOW = "\u001B[43m";
public static final String ANSI_BG_BLUE = "\u001B[44m";
public static final String ANSI_BG_PURPLE = "\u001B[45m";
public static final String ANSI_BG_CYAN = "\u001B[46m";
public static final String ANSI_BG_WHITE = "\u001B[47m";

    protected boolean printColors;
    protected int nameColWidth = 10;
    protected ArrayList<Row> rows;
    protected int width;
    
    protected List<String> columns = new ArrayList<>();
    protected Map<String,Column> columnDefinitions = new HashMap<>();

    private static class Column {

        String name;
        String format;
        String defaultValue;
        Integer width;
 
        public Column(String name, String format, String defaultValue) {
           this.name = name;
           this.format = format;
           this.defaultValue = defaultValue;
        }
    }

    private static class Row {
 
       String name;
       String[][] colors; 
       char[] chr;
       List<Set<String>> table;   
       Map<String,Object> columns = new HashMap<String,Object>();

       Row(String name, int width) {
           this.name = name;
           this.table = new ArrayList<Set<String>>(width);
           for (int i = 0; i < width; ++i) {
               this.table.add(new HashSet<String>());
           }
           this.colors = new String[width][];
           this.chr = new char[width];
       }

    }


    public Ascii(int w, List<String> rows, boolean color) {
        this.width = w;
        this.printColors = color;
        this.rows = new ArrayList<Row>(rows.size());
        for (String name: rows) {
            if (name.length() > nameColWidth) nameColWidth = name.length();
            this.rows.add(new Row(name, this.width));
        }
    }

    public void sort(String col, boolean asc) {
        Comparator<Row> comp = new ColumnComparator(col);
        if (!asc) comp = Collections.reverseOrder(comp);
        Collections.sort(rows, comp);
    }

    protected class ColumnComparator implements Comparator<Row> {

        private final String col;

        public ColumnComparator(String col) { this.col = col; }
 
        @Override
        public int compare(Row r1, Row r2) {
            Object v1 = r1.columns.get(col);
            Object v2 = r2.columns.get(col);
            if (v1 == null && v2 == null) return 0;
            if (v1 == null) return 1;
            if (v2 == null) return -1;
            Comparable c1 = (v1 instanceof Comparable) ? (Comparable) v1 : v1.toString();
            Comparable c2 = (v2 instanceof Comparable) ? (Comparable) v2 : v2.toString();         
            return c1.compareTo(c2);
        } 
    }

    public void addColumn(String name, String format, String def) {
        this.columns.add(name);
        this.columnDefinitions.put(name, new Column(name, format, def));
    }

    public void setColumnValue(int y, String col, Object value) {
        rows.get(y).columns.put(col, value);

        // keep record of max len of values for a column
        Column c = columnDefinitions.get(col);
        Integer len = String.format(c.format, value).length();
        if (c.width == null || len > c.width) {
            c.width = len;
        }
    }

    public Object getColumnValue(int y, String col) {
        return rows.get(y).columns.get(col);
    }

    public Set<String> get(int x, int y) {
        return rows.get(y).table.get(x);
    }

    public void add(int x, int y, String value) {
        rows.get(y).table.get(x).add(value);
    }

    public void setColor(int x, int y, char chr, String... codes) {
        rows.get(y).colors[x] = codes;
        rows.get(y).chr[x] = chr;
    }        


    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();

        b.append(String.format(" %-" + nameColWidth + "s", "NAME"));
        for (String col: columns) {
            Column c = columnDefinitions.get(col);
            if (c.width != null) {
                b.append(String.format(" %" + c.width + "s", col));
                b.append(" ");
            } else {
                // no values, no header either
            }
        }
        b.append("\n");

        for (int i = 0; i < rows.size(); ++i) {
            Row row = rows.get(i);
            b.append(String.format(" %-" + nameColWidth + "s", row.name));
            b.append(" ");

            int idx = 0;
            for (String col: columns) {
                Column c = columnDefinitions.get(col);
                Object val = row.columns.get(col);
                String str;
                if (val == null) {
                    str = c.defaultValue;
                } else {
                    str = String.format(c.format, val);
                }
                if (c.width != null) {
                    b.append(String.format(" %" + c.width + "s", str));
                } else {
                    b.append(str);
                }
                b.append(" ");
                ++idx;
            }

            for (int j = 0; j < width; ++j) {
                boolean col = false;
                if (printColors && row.colors[j] != null) {
                    col = true;
                    for (int k = 0; k < row.colors[j].length; ++k) {
                        b.append(row.colors[j][k]);
                    }
                }
                char chr = row.chr[j] != 0 ? row.chr[j] : ' ';
                b.append(chr);
                if (col) {
                    b.append(ANSI_RESET);
                }
            }
            b.append("\n");
        }
        return b.toString();
    }

    public static void line(int size) {
        for (int i = 0; i < size; ++i) { 
            System.out.print("-");
        }
        System.out.println("");
    }    

}
