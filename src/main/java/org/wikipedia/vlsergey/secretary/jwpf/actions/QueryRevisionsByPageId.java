package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByPageId extends AbstractQueryRevisionsAction implements MultiAction<ParsedPage> {

	private final Direction direction;

	private Long nextRevision;

	private final Long pageId;

	private final RevisionPropery[] properties;

	public QueryRevisionsByPageId(boolean bot, Long pageId, Long rvstartid, Direction direction,
			RevisionPropery[] properties) {
		super(bot, properties);

		log.info("[action=query; prop=revisions]: " + pageId + "; " + rvstartid + "; " + direction + "; "
				+ Arrays.toString(properties));

		this.pageId = pageId;
		this.direction = direction;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");
		setParameter(multipartEntity, "pageids", "" + pageId);

		if (direction != null)
			setParameter(multipartEntity, "rvdir", direction.getQueryString());

		if (rvstartid != null)
			setParameter(multipartEntity, "rvstartid", "" + rvstartid);

		setParameter(multipartEntity, "rvlimit", "" + getLimit());
		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (nextRevision == null)
			return null;

		return new QueryRevisionsByPageId(isBot(), pageId, nextRevision, direction, properties);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element revisionsElement = (Element) queryContinueElement.getElementsByTagName("revisions").item(0);
		nextRevision = new Long(revisionsElement.getAttribute("rvcontinue"));
	}

}
