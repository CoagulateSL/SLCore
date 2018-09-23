package net.coagulate.SL.Data;

import net.coagulate.SL.Database.Database;

/**
 *
 * @author Iain Price
 */
public abstract class IdentifiableTable {
    public abstract String getTableName();
    public abstract int getId();
    
    public int lock(int seconds) { return Database.lock(getTableName(),getId(),seconds); }
    public int lock() { return Database.lock(getTableName(),getId()); }
    public void unlock(int serial) { Database.unlock(getTableName(),getId(),serial); }
    public void extendLock(int serial,int seconds) { Database.extendLock(getTableName(), getId(), serial, seconds); }
    protected int getInt(String column) { return Database.dqi(true, "select "+column+" from "+getTableName()+" where id=?",getId()); }
    protected String getString(String column) { return Database.dqs(true, "select "+column+" from "+getTableName()+" where id=?",getId()); }
    
}
