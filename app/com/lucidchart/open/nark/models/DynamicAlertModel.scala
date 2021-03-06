package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{DynamicAlert, Comparisons}

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import java.math.BigDecimal
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object DynamicAlertModel extends DynamicAlertModel
class DynamicAlertModel extends AppModel {

	protected val dynamicAlertsRowParser = {
		get[UUID]("id") ~
		get[String]("name") ~
		get[UUID]("user_id") ~
		get[Date]("created") ~
		get[String]("search_target") ~
		get[String]("match") ~
		get[String]("build_target") ~
		get[BigDecimal]("error_threshold") ~
		get[BigDecimal]("warn_threshold") ~
		get[Int]("comparison") ~
		get[Boolean]("active") ~
		get[Boolean]("deleted") ~
		get[Int]("frequency") ~
		get[Int]("data_seconds") ~
		get[Int]("drop_null_points") ~
		get[Boolean]("drop_null_targets") map {
			case id ~ name ~ userId ~ created ~ searchTarget ~ matchExpr ~ buildTarget ~ errorThreshold ~ warnThreshold ~ comparison ~ active ~ deleted ~ frequency ~ dataSeconds ~ dropNullPoints ~ dropNullTargets =>
				new DynamicAlert(id, name, userId, searchTarget, matchExpr, buildTarget, Comparisons(comparison), active, deleted, created, frequency, warnThreshold, errorThreshold, dataSeconds, dropNullPoints, dropNullTargets)
		}
	}

	/**
	 * Create a new dynamic alert using the details in the DynamicAlert object
	 * throws an exception on failure
	 * @param alert the dynamic alert to create
	 */
	def createDynamicAlert(alert: DynamicAlert): Unit = {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `dynamic_alerts` (`id`, `name`, `user_id`, `created`, `search_target`, `match`, `build_target`, `error_threshold`, `warn_threshold`, `comparison`, `active`, `deleted`, `frequency`, `data_seconds`, `drop_null_points`, `drop_null_targets`)
				VALUES ({id}, {name}, {user_id}, {created}, {search_target}, {match}, {build_target}, {error_threshold}, {warn_threshold}, {comparison}, {active}, {deleted}, {frequency}, {data_seconds}, {drop_null_points}, {drop_null_targets})
			""").on(
				"id" -> alert.id,
				"name" -> alert.name,
				"user_id" -> alert.userId,
				"created" -> alert.created,
				"search_target" -> alert.searchTarget,
				"match" -> alert.matchExpr,
				"build_target" -> alert.buildTarget,
				"error_threshold" -> alert.errorThreshold,
				"warn_threshold" -> alert.warnThreshold,
				"comparison" -> alert.comparison.id,
				"active" -> alert.active,
				"deleted" -> alert.deleted,
				"frequency" -> alert.frequency,
				"data_seconds" -> alert.dataSeconds,
				"drop_null_points" -> alert.dropNullPoints,
				"drop_null_targets" -> alert.dropNullTargets
			).executeUpdate()(connection)
		}
	}

	/**
	 * Search for dynamic alert records by a search term
	 * @param name the search term to search by
	 * @param page the page of search results to return
	 * @return a List of DynamicAlert records
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
			""").on(
				"name" -> name
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(dynamicAlertsRowParser *)(connection)

			(found, matches)
		}
	}

	/**
	 * Get all deleted dynamic alerts for a user
	 * @param userId the id of the user
	 * @param name the name of the dynamic alert to search for
	 * @param page the page of deleted dynamic alerts to return
	 * @return the number of matching dynamic alerts and a list of matching dynamic alerts
	 */
	def searchDeleted(userId: UUID, name: String, page: Int): (Long, List[DynamicAlert]) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
			""").on(
				"name" -> name,
				"user_id" -> userId
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"user_id" -> userId,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(dynamicAlertsRowParser *)(connection)

			(found, matches)
		}
	}

	/**
	 * Get the dynamic alert specified by the uuid
	 * @param id the uuid of the dynamic alert to get
	 * @return the requested dynamic alert
	 */
	def findDynamicAlertByID(id: UUID): Option[DynamicAlert] = {
		findDynamicAlertByID(List(id)).headOption
	}

	/**
	 * Get the dynamic alerts specified by the uuids passed in
	 * @param ids the ids of the dynamic alerts to get
	 * @return the requested dynamic alerts
	 */
	def findDynamicAlertByID(ids: List[UUID]): List[DynamicAlert] = {
		if (ids.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `dynamic_alerts`
					WHERE `id` IN ({ids})
				""").onList(
					"ids" -> ids
				).toSQL.as(dynamicAlertsRowParser *)(connection)
			}
		}
	}

	/**
	 * Get all active non deleted dynamic alerts
	 * @return the list of active non deleted dynamic alerts
	 */
	def findActiveDynamicAlerts(): List[DynamicAlert] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `dynamic_alerts`
				WHERE `active` = TRUE AND `deleted` = FALSE
			""").as(dynamicAlertsRowParser *)(connection)
		}
	}

	/**
	 * Edit a particular dynamic alert
	 * @param alert the edited alert
	 */
	def editDynamicAlert(alert: DynamicAlert) = {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `dynamic_alerts`
				SET `name`={name}, `search_target`={search_target}, `match`={match_expr}, `build_target`={build_target}, `error_threshold`={error_threshold}, `warn_threshold`={warn_threshold}, `comparison`={comparison}, `active`={active}, `deleted`={deleted}, `frequency`={frequency}, `data_seconds`={data_seconds}, `drop_null_points`={drop_null_points}, `drop_null_targets`={drop_null_targets}
				WHERE `id`={id}
			""").on(
				"name" -> alert.name,
				"search_target" -> alert.searchTarget,
				"match_expr" -> alert.matchExpr,
				"build_target" -> alert.buildTarget,
				"error_threshold" -> alert.errorThreshold,
				"warn_threshold" -> alert.warnThreshold,
				"comparison" -> alert.comparison.id,
				"active" -> alert.active,
				"deleted" -> alert.deleted,
				"frequency" -> alert.frequency,
				"data_seconds" -> alert.dataSeconds,
				"drop_null_points" -> alert.dropNullPoints,
				"drop_null_targets" -> alert.dropNullTargets,
				"id" -> alert.id
			).executeUpdate()(connection)
		}
	}
}
