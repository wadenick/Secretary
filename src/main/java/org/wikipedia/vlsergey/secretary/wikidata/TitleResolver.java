package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Label;

public class TitleResolver implements Function<EntityId, String> {

	private final Map<EntityId, String> cache = new HashMap<>();

	private final Locale locale;

	private final WikiCache wikidataCache;

	public TitleResolver(WikiCache wikidataCache, Locale locale) {
		this.wikidataCache = wikidataCache;
		this.locale = locale;
	}

	@Override
	public String apply(EntityId t) {
		if (locale.getLanguage().equals(new Locale("ru").getLanguage())) {
			if (t.equals(Places.РСФСР)) {
				return "РСФСР";
			} else if (t.equals(Places.СССР)) {
				return "СССР";
			} else if (t.equals(Places.США)) {
				return "США";
			}
		}

		synchronized (this) {
			if (cache.containsKey(t)) {
				return cache.get(t);
			}
			String value = build(t);
			cache.put(t, value);
			return value;
		}
	}

	private String build(final Entity entity) {
		Label localized = entity.getLabel(locale.getLanguage());
		if (localized != null)
			return localized.getValue();
		Label en = entity.getLabel("en");
		if (en != null)
			return en.getValue();
		return entity.getId().getPageTitle();
	}

	private String build(EntityId entityId) {
		Revision revision = wikidataCache.queryLatestRevision(entityId.getPageTitle());
		if (revision == null) {
			return entityId.toString();
		}
		return build(new Entity(revision));
	}

	public void update(Entity entity) {
		synchronized (this) {
			cache.put(entity.getId(), build(entity));
		}
	}
}