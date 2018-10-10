package net.coagulate.SL.Data;

/**
 *
 * @author Iain Price
 */
public class LockTest extends LockableTable {

    public LockTest(int id) { super(id); }

    @Override
    public String getTableName() { return "locktest"; }
    
}
