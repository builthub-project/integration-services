package com.nttdata.builthub.sparql;

import java.util.Map;

import org.springframework.util.StringUtils;

public class MimeTypeUtils {
	private static final Map<String, String> MIMETYPES = Map.of("application/sparql-results+json", "json", "text/csv",
			"csv", "text/tab-separated-values", "tsv", "application/sparql-results+xml", "xml",
			"application/x-binary-rdf-results-table", "bin", "application/json", "json", "application/xml", "xml");

	public static final String getFileExtension(final String mimeType) {
		String result = "txt";

		if (StringUtils.hasText(mimeType)) {
			String value = MIMETYPES.get(mimeType);
			if (StringUtils.hasText(value)) {
				result = value;
			}
		}

		return result;
	}

	public static final boolean isSupported(final String mimeType) {
		if (StringUtils.hasText(mimeType)) {
			return MIMETYPES.containsKey(mimeType);
		}

		return false;
	}
	
	public static final String getResponseFormat(final String resultFormat, final String defaultFormat) {
		String responseType = defaultFormat;
		if (StringUtils.hasText(resultFormat)) {
			if (MIMETYPES.containsKey(resultFormat)) {
				responseType = resultFormat;
			}
		}

		return responseType;
	}
}
