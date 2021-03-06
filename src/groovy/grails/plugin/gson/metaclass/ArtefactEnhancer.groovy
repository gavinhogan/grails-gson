package grails.plugin.gson.metaclass

import java.util.Map.Entry
import javax.servlet.http.HttpServletRequest
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import grails.plugin.gson.converters.GSON
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.*

/**
 * Adds GSON meta methods and properties to Grails artifacts.
 */
@Slf4j
class ArtefactEnhancer {

	private final GrailsApplication grailsApplication
	private final Gson gson

	ArtefactEnhancer(GrailsApplication grailsApplication, GsonBuilder gsonBuilder) {
		this.grailsApplication = grailsApplication
		gson = gsonBuilder.create()
	}

	void enhanceControllers() {
		for (controller in grailsApplication.controllerClasses) {
			controller.metaClass.render = { GSON gson ->
				gson.render delegate.response
			}
		}
	}

	void enhanceDomains() {
		grailsApplication.domainClasses.each { GrailsDomainClass domainClass ->
			domainClass.metaClass.constructor = { JsonObject json ->
				gson.fromJson(json, delegate)
			}
			domainClass.metaClass.setProperties = { JsonObject json ->
				for (Entry<String, JsonElement> entry in json.entrySet()) {
					def persistentProperty = domainClass.getPersistentProperty(entry.key)
					def adapter = gson.getAdapter(TypeToken.get(persistentProperty.type))
					delegate[entry.key] = adapter.fromJsonTree(entry.value)
				}
			}
		}
	}

	void enhanceRequest() {
		def requestMetaClass = GroovySystem.metaClassRegistry.getMetaClass(HttpServletRequest)
		requestMetaClass.getGSON = {->
			GSON.parse(delegate)
		}
	}
}
