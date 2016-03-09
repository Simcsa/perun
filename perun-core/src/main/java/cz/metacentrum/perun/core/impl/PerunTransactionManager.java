package cz.metacentrum.perun.core.impl;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.transaction.*;

public class PerunTransactionManager extends DataSourceTransactionManager implements ResourceTransactionManager, InitializingBean {

	private static final long serialVersionUID = 1L;

	private Auditer auditer;
	private CacheManager cacheManager;

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		this.getAuditer().newTopLevelTransaction();
		this.getCacheManager().newTopLevelTransaction();
		super.doBegin(transaction, definition);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		try {
			this.getCacheManager().getCacheTransactionManager().commit();
		} catch (RollbackException e) {
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
		super.doCommit(status);
		this.getAuditer().flush();
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		try {
			this.getCacheManager().getCacheTransactionManager().rollback();
		} catch (SystemException e) {
			e.printStackTrace();
		}
		super.doRollback(status);
		this.getAuditer().clean();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		super.doCleanupAfterCompletion(transaction);
		this.getAuditer().clean();
	}

	public Auditer getAuditer() {
		return this.auditer;
	}

	public void setAuditer(Auditer auditer) {
		this.auditer = auditer;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
}
