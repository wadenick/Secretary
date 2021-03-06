package org.wikipedia.vlsergey.secretary.cache;

import java.sql.SQLException;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Repository
public class StoredRevisionDao {

	@Autowired
	private StoredPageDao storedPageDao;

	protected HibernateTemplate template = null;

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public int clear(final Project project) {
		int a = template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery query = session.createSQLQuery("DELETE FROM revision " + "WHERE page_project=?");
				query.setParameter(0, project.getCode());
				return query.executeUpdate();
			}
		});
		int b = template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery query = session.createSQLQuery("DELETE FROM revision " + "WHERE project=?");
				query.setParameter(0, project.getCode());
				return query.executeUpdate();
			}
		});
		return a + b;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public StoredRevision getOrCreate(Project project, Revision withContent) {
		final StoredRevisionPk key = new StoredRevisionPk(project, withContent.getId());
		StoredRevision revisionImpl = getRevisionById(key);
		if (revisionImpl == null) {
			revisionImpl = new StoredRevision();
			revisionImpl.setKey(key);
			template.persist(revisionImpl);
			revisionImpl = template.get(StoredRevision.class, key);
			// template.flush();
		}

		if (withContent.getPage() != null) {

			StoredPage storedPage;
			if (revisionImpl.getPage() == null) {
				storedPage = storedPageDao.getOrCreate(project, withContent.getPage());
				revisionImpl.setPage(storedPage);
			} else {
				storedPage = revisionImpl.getPage();
			}

		}

		if (updateRequired(withContent.getAnon(), revisionImpl.getAnon())) {
			revisionImpl.setAnon(withContent.getAnon());
		}
		if (updateRequired(withContent.getBot(), revisionImpl.getBot())) {
			revisionImpl.setBot(withContent.getBot());
		}
		if (updateRequired(withContent.getComment(), revisionImpl.getComment())) {
			revisionImpl.setComment(withContent.getComment());
		}
		if (withContent.getContent() != null && updateRequired(withContent.getContent(), revisionImpl.getContent())) {
			revisionImpl.setContent(withContent.getContent());
		}
		if (updateRequired(withContent.getMinor(), revisionImpl.getMinor())) {
			revisionImpl.setMinor(withContent.getMinor());
		}
		if (updateRequired(withContent.getSize(), revisionImpl.getSize())) {
			revisionImpl.setSize(withContent.getSize());
		}
		if (updateRequired(withContent.getTimestamp(), revisionImpl.getTimestamp())) {
			revisionImpl.setTimestamp(withContent.getTimestamp());
		}
		if (updateRequired(withContent.getUser(), revisionImpl.getUser())) {
			revisionImpl.setUser(withContent.getUser());
		}
		if (updateRequired(withContent.getUserId(), revisionImpl.getUserId())) {
			revisionImpl.setUserId(withContent.getUserId());
		}
		if (updateRequired(withContent.getUserHidden(), revisionImpl.getUserHidden())) {
			revisionImpl.setUserHidden(withContent.getUserHidden());
		}
		if (withContent.getXml() != null && updateRequired(withContent.getXml(), revisionImpl.getXml())) {
			revisionImpl.setXml(withContent.getXml());
		}

		return revisionImpl;
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public StoredRevision getRevisionById(Project project, Long revisionId) {
		return getRevisionById(new StoredRevisionPk(project, revisionId));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public StoredRevision getRevisionById(StoredRevisionPk key) {
		return template.get(StoredRevision.class, key);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public int removePageRevisionsExcept(final Project project, final Long pageId, Set<Long> preserveRevisionIds) {

		final StringBuilder idsBuilder = new StringBuilder();
		for (Long id : preserveRevisionIds) {
			if (idsBuilder.length() != 0) {
				idsBuilder.append(",");
			}
			idsBuilder.append(id);
		}

		int updated = template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery query = session.createSQLQuery("DELETE FROM revision "
						+ "WHERE project=? AND page_project=? AND page_pageid=? AND revisionid NOT IN ("
						+ idsBuilder.toString() + ")");
				query.setParameter(0, project.getCode());
				query.setParameter(1, project.getCode());
				query.setParameter(2, pageId);
				return query.executeUpdate();
			}
		});

		return updated;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	private boolean updateRequired(final Object newValue, final Object oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

}
