package net.coagulate.SL.Data;

import net.coagulate.Core.Tools.SystemException;

/**
 * @author Iain Price
 */
public class LockTest extends LockableTable {

	public LockTest(int id) {
		super(id);
		Integer count = dqi(true, "select count(*) from locktest where id=?", id);
		if (count == 0) { throw new SystemException("Lock test row " + id + " does not exist"); }
	}

	@Override
	public String getTableName() { return "locktest"; }

}
