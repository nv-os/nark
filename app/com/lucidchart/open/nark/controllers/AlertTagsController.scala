package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppAction, AuthAction}
import com.lucidchart.open.nark.models.{AlertModel, AlertTagModel, TagSubscriptionModel}
import com.lucidchart.open.nark.models.AlertTagConverter
import com.lucidchart.open.nark.views
import play.api.libs.json.Json

object AlertTagsController extends AlertTagsController
class AlertTagsController extends AppController {
	
	/*
	 *	Get tag and all the dashboards it is assocaited with.
	 * @param tag the tag to look for
	 */
	def tag(tag: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val alertIds = AlertTagModel.findAlertsByTag(tag).map(_.alertId)
			val alerts = AlertModel.getAlerts(alertIds)
			val subscriptions = TagSubscriptionModel.getSubscriptionsByTag(tag)
			Ok(views.html.alerttags.tag(tag, alerts, subscriptions))
		}
	}

	/**
	 * Search for a specific tag
	 * @param term the search term to look for tags by
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, tags) = AlertTagModel.search(term, realPage - 1)
			val alertTags = AlertTagModel.findAlertsByTag(tags.map{_.tag})
			val alerts = AlertModel.getAlerts(alertTags.map(_.alertId).distinct).filter(!_.deleted)
			Ok(views.html.alerttags.search(term, realPage, AlertTagModel.configuredLimit, found, AlertTagConverter.toTagMap(alertTags, alerts)))
		}
	}

	/*
	 * Search tags by name. Returns json formatted for jquery-tokeninput.
	 */
	def searchToJson(term: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val (found, matches) = AlertTagModel.search(term, 1)
			Ok(Json.toJson(matches.map{ m =>
				Json.obj("id" -> m.alertId.toString, "name" -> m.tag)
			}))
		}
	}
}