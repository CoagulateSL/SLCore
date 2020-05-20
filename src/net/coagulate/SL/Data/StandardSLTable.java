package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.StandardTableRow;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

public abstract class StandardSLTable extends StandardTableRow {
	public StandardSLTable(int id) {super(id);}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public DBConnection getDatabase() {
		return SL.getDB();
	}

	@Nonnull
	@Override
	public abstract String getTableName();
}
