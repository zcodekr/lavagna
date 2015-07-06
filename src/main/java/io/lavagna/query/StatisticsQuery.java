/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lavagna.query;

import io.lavagna.model.CardFull;
import io.lavagna.model.EventsCount;
import io.lavagna.model.LabelAndValueWithCount;
import io.lavagna.model.MilestoneCount;
import io.lavagna.model.StatisticForExport;
import io.lavagna.model.StatisticsResult;

import java.util.Date;
import java.util.List;

import ch.digitalfondue.npjt.Bind;
import ch.digitalfondue.npjt.QueriesOverride;
import ch.digitalfondue.npjt.Query;
import ch.digitalfondue.npjt.QueryOverride;
import ch.digitalfondue.npjt.QueryRepository;

@QueryRepository
public interface StatisticsQuery {

	@Query("INSERT INTO LA_BOARD_STATISTICS SELECT :date, BOARD_ID, BOARD_COLUMN_DEFINITION_ID_FK, BOARD_COLUMN_LOCATION, COUNT(*) AS CARDS_COUNT FROM LA_CARD "
			+ "INNER JOIN LA_BOARD_COLUMN ON LA_BOARD_COLUMN.BOARD_COLUMN_ID = LA_CARD.CARD_BOARD_COLUMN_ID_FK "
			+ "INNER JOIN LA_BOARD ON LA_BOARD_COLUMN.BOARD_COLUMN_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "GROUP BY BOARD_ID, BOARD_COLUMN_DEFINITION_ID_FK, BOARD_COLUMN_LOCATION")
	void snapshotCardsStatus(@Bind("date") Date date);

	@Query("DELETE FROM LA_BOARD_STATISTICS WHERE BOARD_STATISTICS_TIME NOT IN (SELECT DAY FROM LA_BOARD_STATISTICS_DAYS)")
	@QueriesOverride({
			@QueryOverride(db = DB.MYSQL, value = "DELETE BOARD_STATS FROM LA_BOARD_STATISTICS AS BOARD_STATS LEFT JOIN LA_BOARD_STATISTICS_DAYS ON BOARD_STATS.BOARD_STATISTICS_TIME = LA_BOARD_STATISTICS_DAYS.DAY WHERE LA_BOARD_STATISTICS_DAYS.DAY IS NULL")})
	void cleanOldCardsStatusSnapshots();


	@Query("SELECT BOARD_STATISTICS_TIME AS TIME, BOARD_COLUMN_DEFINITION_VALUE, SUM(BOARD_STATISTICS_COUNT) AS STATISTICS_COUNT FROM LA_BOARD_STATISTICS "
			+ "INNER JOIN LA_BOARD_COLUMN_DEFINITION ON BOARD_STATISTICS_COLUMN_DEFINITION_ID_FK = BOARD_COLUMN_DEFINITION_ID "
			+ "INNER JOIN LA_BOARD_STATISTICS_DAYS ON BOARD_STATISTICS_TIME = DAY "
			+ "INNER JOIN LA_BOARD ON BOARD_STATISTICS_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "WHERE BOARD_STATISTICS_LOCATION = 'BOARD' AND BOARD_STATISTICS_BOARD_ID_FK = :boardId AND BOARD_STATISTICS_TIME >= :fromDate "
			+ "GROUP BY BOARD_STATISTICS_TIME, BOARD_COLUMN_DEFINITION_VALUE")
	List<StatisticsResult> getCardsStatusByBoard(@Bind("boardId") int boardId, @Bind("fromDate") Date fromDate);

	@Query("SELECT BOARD_STATISTICS_TIME, BOARD_COLUMN_DEFINITION_VALUE, BOARD_STATISTICS_LOCATION, BOARD_STATISTICS_COUNT FROM LA_BOARD_STATISTICS "
			+ "INNER JOIN LA_BOARD_COLUMN_DEFINITION ON BOARD_STATISTICS_COLUMN_DEFINITION_ID_FK = BOARD_COLUMN_DEFINITION_ID WHERE BOARD_STATISTICS_BOARD_ID_FK = :boardId")
	List<StatisticForExport> findForBoard(@Bind("boardId") int boardId);

	@Query("INSERT INTO LA_BOARD_STATISTICS(BOARD_STATISTICS_TIME, BOARD_STATISTICS_BOARD_ID_FK, BOARD_STATISTICS_COLUMN_DEFINITION_ID_FK, BOARD_STATISTICS_LOCATION, BOARD_STATISTICS_COUNT) "
			+ " VALUES(:date, :boardId, :boardColumnDefinitionId, :location, :count)")
	int addFromImport(@Bind("date") Date date, @Bind("boardId") int boardId,
			@Bind("boardColumnDefinitionId") int boardColumnDefinitionId, @Bind("location") String location,
			@Bind("count") long count);

	@Query("SELECT BOARD_STATISTICS_TIME AS TIME, BOARD_COLUMN_DEFINITION_VALUE, SUM(BOARD_STATISTICS_COUNT) AS STATISTICS_COUNT FROM LA_BOARD_STATISTICS "
			+ "INNER JOIN LA_BOARD_COLUMN_DEFINITION ON BOARD_STATISTICS_COLUMN_DEFINITION_ID_FK = BOARD_COLUMN_DEFINITION_ID "
			+ "INNER JOIN LA_BOARD_STATISTICS_DAYS ON BOARD_STATISTICS_TIME = DAY "
			+ "INNER JOIN LA_BOARD ON BOARD_STATISTICS_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "WHERE BOARD_STATISTICS_LOCATION = 'BOARD' AND BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND BOARD_STATISTICS_TIME >= :fromDate "
			+ "GROUP BY BOARD_STATISTICS_TIME, BOARD_COLUMN_DEFINITION_VALUE")
	List<StatisticsResult> getCardsStatusByProject(@Bind("projectId") int projectId, @Bind("fromDate") Date fromDate);

	@Query("SELECT COUNT(*) FROM (SELECT DISTINCT EVENT_USER_ID_FK FROM LA_PROJECT "
			+ "INNER JOIN LA_CARD_FULL ON LA_CARD_FULL.PROJECT_SHORT_NAME = LA_PROJECT.PROJECT_SHORT_NAME "
			+ "INNER JOIN LA_BOARD ON LA_CARD_FULL.BOARD_SHORT_NAME = LA_BOARD.BOARD_SHORT_NAME "
			+ "INNER JOIN LA_EVENT ON EVENT_CARD_ID_FK = LA_CARD_FULL.CARD_ID "
			+ "WHERE BOARD_ID = :boardId AND EVENT_TIME >= :fromDate) AS USERS")
	Integer getActiveUsersOnBoard(@Bind("boardId") int boardId, @Bind("fromDate") Date fromDate);

	// Average users per card

	@Query("SELECT COUNT(*) FROM (SELECT DISTINCT EVENT_USER_ID_FK FROM LA_PROJECT "
			+ "INNER JOIN LA_CARD_FULL ON LA_CARD_FULL.PROJECT_SHORT_NAME = LA_PROJECT.PROJECT_SHORT_NAME "
			+ "INNER JOIN LA_BOARD ON LA_CARD_FULL.BOARD_SHORT_NAME = LA_BOARD.BOARD_SHORT_NAME "
			+ "INNER JOIN LA_EVENT ON EVENT_CARD_ID_FK = LA_CARD_FULL.CARD_ID "
			+ "WHERE BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND EVENT_TIME >= :fromDate) AS USERS")
	Integer getActiveUsersOnProject(@Bind("projectId") int projectId, @Bind("fromDate") Date fromDate);

	@Query("SELECT AVG(USERS) FROM ( " + "SELECT CARD_ID, COUNT(ASSIGNED_USER_ID) AS USERS " + "FROM LA_CARD_FULL "
			+ "LEFT JOIN LA_ASSIGNED_CARD ON LA_CARD_FULL.CARD_ID = LA_ASSIGNED_CARD.ASSIGNED_CARD_ID "
			+ "INNER JOIN LA_BOARD ON LA_CARD_FULL.BOARD_SHORT_NAME = LA_BOARD.BOARD_SHORT_NAME "
			+ "WHERE BOARD_ID = :boardId AND BOARD_COLUMN_LOCATION = 'BOARD' " + "GROUP BY CARD_ID) AS ASSIGNED ")
	Double getAverageUsersPerCardOnBoard(@Bind("boardId") int boardId);

	@Query("SELECT AVG(USERS) FROM ( " + "SELECT CARD_ID, COUNT(ASSIGNED_USER_ID) AS USERS " + "FROM LA_CARD_FULL "
			+ "LEFT JOIN LA_ASSIGNED_CARD ON LA_CARD_FULL.CARD_ID = LA_ASSIGNED_CARD.ASSIGNED_CARD_ID "
			+ "INNER JOIN LA_BOARD ON LA_CARD_FULL.BOARD_SHORT_NAME = LA_BOARD.BOARD_SHORT_NAME "
			+ "WHERE BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY CARD_ID) AS ASSIGNED ")
	Double getAverageUsersPerCardOnProject(@Bind("projectId") int projectId);

	// Average cards per user

	@Query("SELECT AVG(CARDS) FROM ( " + "SELECT COUNT(ASSIGNED_CARD_ID) AS CARDS, ASSIGNED_USER_ID "
			+ "FROM LA_ASSIGNED_CARD "
			+ "INNER JOIN LA_CARD_FULL ON LA_CARD_FULL.CARD_ID = LA_ASSIGNED_CARD.ASSIGNED_CARD_ID "
			+ "INNER JOIN LA_BOARD ON LA_CARD_FULL.BOARD_SHORT_NAME = LA_BOARD.BOARD_SHORT_NAME "
			+ "WHERE BOARD_ID = :boardId AND BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY ASSIGNED_USER_ID) AS ASSIGNED ")
	Double getAverageCardsPerUserOnBoard(@Bind("boardId") int boardId);

	@Query("SELECT AVG(CARDS) FROM ( " + "SELECT COUNT(ASSIGNED_CARD_ID) AS CARDS, ASSIGNED_USER_ID "
			+ "FROM LA_ASSIGNED_CARD "
			+ "INNER JOIN LA_CARD_FULL ON LA_CARD_FULL.CARD_ID = LA_ASSIGNED_CARD.ASSIGNED_CARD_ID "
			+ "INNER JOIN LA_BOARD ON LA_CARD_FULL.BOARD_SHORT_NAME = LA_BOARD.BOARD_SHORT_NAME "
			+ "WHERE BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY ASSIGNED_USER_ID) AS ASSIGNED ")
	Double getAverageCardsPerUserOnProject(@Bind("projectId") int projectId);

	// Cards by label

	@Query("SELECT CARD_LABEL_ID, CARD_LABEL_NAME, CARD_LABEL_COLOR, "
			+ "CARD_LABEL_VALUE_TYPE, CARD_LABEL_VALUE_LIST_VALUE_FK, COUNT(*) AS LABEL_COUNT FROM LA_CARD "
			+ "INNER JOIN LA_CARD_LABEL_VALUE ON LA_CARD_LABEL_VALUE.CARD_ID_FK = LA_CARD.CARD_ID "
			+ "INNER JOIN LA_CARD_LABEL ON LA_CARD_LABEL.CARD_LABEL_ID = LA_CARD_LABEL_VALUE.CARD_LABEL_ID_FK "
			+ "INNER JOIN LA_BOARD_COLUMN ON LA_BOARD_COLUMN.BOARD_COLUMN_ID = LA_CARD.CARD_BOARD_COLUMN_ID_FK "
			+ "INNER JOIN LA_BOARD ON BOARD_COLUMN_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "WHERE LA_CARD_LABEL.CARD_LABEL_DOMAIN = 'USER' AND BOARD_ID = :boardId "
			+ "AND LA_CARD_LABEL_VALUE.CARD_LABEL_VALUE_DELETED = FALSE AND BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY CARD_LABEL_ID, CARD_LABEL_NAME, CARD_LABEL_COLOR, CARD_LABEL_VALUE_TYPE, CARD_LABEL_VALUE_LIST_VALUE_FK "
			+ "ORDER BY CARD_LABEL_NAME")
	List<LabelAndValueWithCount> getCardsByLabelOnBoard(@Bind("boardId") int boardId);

	@Query("SELECT CARD_LABEL_ID, CARD_LABEL_NAME, CARD_LABEL_COLOR, "
			+ "CARD_LABEL_VALUE_TYPE, CARD_LABEL_VALUE_LIST_VALUE_FK, COUNT(*) AS LABEL_COUNT FROM LA_CARD "
			+ "INNER JOIN LA_CARD_LABEL_VALUE ON LA_CARD_LABEL_VALUE.CARD_ID_FK = LA_CARD.CARD_ID "
			+ "INNER JOIN LA_CARD_LABEL ON LA_CARD_LABEL.CARD_LABEL_ID = LA_CARD_LABEL_VALUE.CARD_LABEL_ID_FK "
			+ "INNER JOIN LA_BOARD_COLUMN ON LA_BOARD_COLUMN.BOARD_COLUMN_ID = LA_CARD.CARD_BOARD_COLUMN_ID_FK "
			+ "INNER JOIN LA_BOARD ON BOARD_COLUMN_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "WHERE LA_CARD_LABEL.CARD_LABEL_DOMAIN = 'USER' AND BOARD_PROJECT_ID_FK = :projectId "
			+ "AND LA_CARD_LABEL_VALUE.CARD_LABEL_VALUE_DELETED = FALSE AND BOARD_ARCHIVED = FALSE AND BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY CARD_LABEL_ID, CARD_LABEL_NAME, CARD_LABEL_COLOR, CARD_LABEL_VALUE_TYPE, CARD_LABEL_VALUE_LIST_VALUE_FK "
			+ "ORDER BY CARD_LABEL_NAME")
	List<LabelAndValueWithCount> getCardsByLabelOnProject(@Bind("projectId") int projectId);

	// Created cards by date

	@Query("SELECT CAST(EVENT_TIME AS DATE) AS EVENT_DATE, COUNT(*) AS EVENT_COUNT FROM LA_EVENT "
			+ "INNER JOIN LA_BOARD_COLUMN ON EVENT_COLUMN_ID_FK = BOARD_COLUMN_ID "
			+ "INNER JOIN LA_BOARD ON BOARD_COLUMN_BOARD_ID_FK = BOARD_ID "
			+ "WHERE EVENT_TYPE = 'CARD_CREATE' AND BOARD_ID = :boardId AND EVENT_TIME >= :fromDate "
			+ "GROUP BY EVENT_DATE ORDER BY EVENT_DATE")
	List<EventsCount> getCreatedCardsByBoard(@Bind("boardId") int boardId, @Bind("fromDate") Date fromDate);

	@Query("SELECT CAST(EVENT_TIME AS DATE) AS EVENT_DATE, COUNT(*) AS EVENT_COUNT FROM LA_EVENT "
			+ "INNER JOIN LA_BOARD_COLUMN ON EVENT_COLUMN_ID_FK = BOARD_COLUMN_ID "
			+ "INNER JOIN LA_BOARD ON BOARD_COLUMN_BOARD_ID_FK = BOARD_ID "
			+ "WHERE EVENT_TYPE = 'CARD_CREATE' AND BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND EVENT_TIME >= :fromDate "
			+ "GROUP BY EVENT_DATE ORDER BY EVENT_DATE")
	List<EventsCount> getCreatedCardsByProject(@Bind("projectId") int projectId, @Bind("fromDate") Date fromDate);

	// Closed cards by date

	@Query("SELECT CAST(EVENT_TIME AS DATE) AS EVENT_DATE, COUNT(*) AS EVENT_COUNT FROM LA_EVENT "
			+ "JOIN LA_BOARD_COLUMN old on LA_EVENT.EVENT_PREV_COLUMN_ID_FK = old.BOARD_COLUMN_ID "
			+ "JOIN LA_BOARD_COLUMN_DEFINITION oldDef on old.BOARD_COLUMN_DEFINITION_ID_FK = oldDef.BOARD_COLUMN_DEFINITION_ID "
			+ "JOIN LA_BOARD_COLUMN new on LA_EVENT.EVENT_COLUMN_ID_FK = new.BOARD_COLUMN_ID "
			+ "JOIN LA_BOARD_COLUMN_DEFINITION newDef on new.BOARD_COLUMN_DEFINITION_ID_FK = newDef.BOARD_COLUMN_DEFINITION_ID "
			+ "JOIN LA_BOARD ON new.BOARD_COLUMN_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "WHERE (EVENT_TYPE = 'CARD_MOVE' OR EVENT_TYPE = 'CARD_ARCHIVE' OR EVENT_TYPE = 'CARD_TRASH') AND "
			+ "BOARD_ID = :boardId AND EVENT_TIME >= :fromDate AND "
			+ "oldDef.BOARD_COLUMN_DEFINITION_VALUE <> 'CLOSED' AND newDef.BOARD_COLUMN_DEFINITION_VALUE = 'CLOSED' "
			+ "GROUP BY EVENT_DATE ORDER BY EVENT_DATE")
	List<EventsCount> getClosedCardsByBoard(@Bind("boardId") int boardId, @Bind("fromDate") Date fromDate);

	@Query("SELECT CAST(EVENT_TIME AS DATE) AS EVENT_DATE, COUNT(*) AS EVENT_COUNT FROM LA_EVENT "
			+ "JOIN LA_BOARD_COLUMN old on LA_EVENT.EVENT_PREV_COLUMN_ID_FK = old.BOARD_COLUMN_ID "
			+ "JOIN LA_BOARD_COLUMN_DEFINITION oldDef on old.BOARD_COLUMN_DEFINITION_ID_FK = oldDef.BOARD_COLUMN_DEFINITION_ID "
			+ "JOIN LA_BOARD_COLUMN new on LA_EVENT.EVENT_COLUMN_ID_FK = new.BOARD_COLUMN_ID "
			+ "JOIN LA_BOARD_COLUMN_DEFINITION newDef on new.BOARD_COLUMN_DEFINITION_ID_FK = newDef.BOARD_COLUMN_DEFINITION_ID "
			+ "JOIN LA_BOARD ON new.BOARD_COLUMN_BOARD_ID_FK = LA_BOARD.BOARD_ID "
			+ "WHERE (EVENT_TYPE = 'CARD_MOVE' OR EVENT_TYPE = 'CARD_ARCHIVE' OR EVENT_TYPE = 'CARD_TRASH') AND "
			+ "BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND EVENT_TIME >= :fromDate AND "
			+ "oldDef.BOARD_COLUMN_DEFINITION_VALUE <> 'CLOSED' AND newDef.BOARD_COLUMN_DEFINITION_VALUE = 'CLOSED' "
			+ "GROUP BY EVENT_DATE ORDER BY EVENT_DATE")
	List<EventsCount> getClosedCardsByProject(@Bind("projectId") int projectId, @Bind("fromDate") Date fromDate);

	// Most active card

	@Query("SELECT CARD_ID, CARD_NAME, CARD_SEQ_NUMBER, CARD_ORDER, CARD_BOARD_COLUMN_ID_FK, CREATE_USER, CREATE_TIME, LAST_UPDATE_USER, LAST_UPDATE_TIME, BOARD_COLUMN_DEFINITION_VALUE, LA_CARD_FULL.BOARD_SHORT_NAME, LA_CARD_FULL.PROJECT_SHORT_NAME, COUNT(*) AS EVENTS_COUNT FROM LA_EVENT "
			+ "INNER JOIN LA_CARD_FULL ON CARD_ID = EVENT_CARD_ID_FK "
			+ "INNER JOIN LA_BOARD_COLUMN ON CARD_BOARD_COLUMN_ID_FK = BOARD_COLUMN_ID "
			+ "INNER JOIN LA_BOARD ON BOARD_COLUMN_BOARD_ID_FK = BOARD_ID "
			+ "WHERE BOARD_ID = :boardId AND EVENT_TIME >= :fromDate AND LA_BOARD_COLUMN.BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY CARD_ID, CARD_NAME, CARD_SEQ_NUMBER, CARD_ORDER, CARD_BOARD_COLUMN_ID_FK, CREATE_USER, CREATE_TIME, LAST_UPDATE_USER, LAST_UPDATE_TIME, BOARD_COLUMN_DEFINITION_VALUE, LA_CARD_FULL.BOARD_SHORT_NAME, PROJECT_SHORT_NAME ORDER BY EVENTS_COUNT DESC LIMIT 1")
	CardFull getMostActiveCardByBoard(@Bind("boardId") int boardId, @Bind("fromDate") Date fromDate);

	@Query("SELECT CARD_ID, CARD_NAME, CARD_SEQ_NUMBER, CARD_ORDER, CARD_BOARD_COLUMN_ID_FK, CREATE_USER, CREATE_TIME, LAST_UPDATE_USER, LAST_UPDATE_TIME, BOARD_COLUMN_DEFINITION_VALUE, LA_CARD_FULL.BOARD_SHORT_NAME, LA_CARD_FULL.PROJECT_SHORT_NAME, COUNT(*) AS EVENTS_COUNT FROM LA_EVENT "
			+ "INNER JOIN LA_CARD_FULL ON CARD_ID = EVENT_CARD_ID_FK "
			+ "INNER JOIN LA_BOARD_COLUMN ON CARD_BOARD_COLUMN_ID_FK = BOARD_COLUMN_ID "
			+ "INNER JOIN LA_BOARD ON BOARD_COLUMN_BOARD_ID_FK = BOARD_ID "
			+ "WHERE BOARD_ARCHIVED = FALSE AND BOARD_PROJECT_ID_FK = :projectId AND EVENT_TIME >= :fromDate AND LA_BOARD_COLUMN.BOARD_COLUMN_LOCATION = 'BOARD' "
			+ "GROUP BY CARD_ID, CARD_NAME, CARD_SEQ_NUMBER, CARD_ORDER, CARD_BOARD_COLUMN_ID_FK, CREATE_USER, CREATE_TIME, LAST_UPDATE_USER, LAST_UPDATE_TIME, BOARD_COLUMN_DEFINITION_VALUE, LA_CARD_FULL.BOARD_SHORT_NAME, PROJECT_SHORT_NAME ORDER BY EVENTS_COUNT DESC LIMIT 1")
	CardFull getMostActiveCardByProject(@Bind("projectId") int projectId, @Bind("fromDate") Date fromDate);

	// Milestones

	@Query("SELECT CAST(EVENT_TIME AS DATE) AS EVENT_DATE, COUNT(*) AS EVENT_COUNT FROM LA_EVENT "
			+ "WHERE EVENT_TYPE = 'LABEL_CREATE' AND EVENT_VALUE_STRING = :milestone AND EVENT_LABEL_NAME = 'MILESTONE' AND EVENT_TIME >= :fromDate "
			+ "GROUP BY EVENT_DATE ORDER BY EVENT_DATE")
	List<EventsCount> getAssignedCardsByMilestone(@Bind("milestone") String milestone, @Bind("fromDate") Date fromDate);

	@Query("SELECT CAST(EVENT_TIME AS DATE) AS EVENT_DATE, COUNT(*) AS EVENT_COUNT FROM LA_EVENT "
			+ "JOIN LA_BOARD_COLUMN old on LA_EVENT.EVENT_PREV_COLUMN_ID_FK = old.BOARD_COLUMN_ID "
			+ "JOIN LA_BOARD_COLUMN_DEFINITION oldDef on old.BOARD_COLUMN_DEFINITION_ID_FK = oldDef.BOARD_COLUMN_DEFINITION_ID "
			+ "JOIN LA_BOARD_COLUMN new on LA_EVENT.EVENT_COLUMN_ID_FK = new.BOARD_COLUMN_ID "
			+ "JOIN LA_BOARD_COLUMN_DEFINITION newDef on new.BOARD_COLUMN_DEFINITION_ID_FK = newDef.BOARD_COLUMN_DEFINITION_ID "
			+ "INNER JOIN LA_CARD_LABEL_VALUE ON EVENT_CARD_ID_FK = CARD_ID_FK AND CARD_LABEL_VALUE_DELETED <> TRUE "
			+ "WHERE (EVENT_TYPE = 'CARD_MOVE' OR EVENT_TYPE = 'CARD_ARCHIVE' OR EVENT_TYPE = 'CARD_TRASH') AND "
			+ "CARD_LABEL_VALUE_LIST_VALUE_FK = :milestoneId AND EVENT_TIME >= :fromDate AND "
			+ "oldDef.BOARD_COLUMN_DEFINITION_VALUE <> 'CLOSED' AND newDef.BOARD_COLUMN_DEFINITION_VALUE = 'CLOSED' "
			+ "GROUP BY EVENT_DATE ORDER BY EVENT_DATE")
	List<EventsCount> getClosedCardsByMilestone(@Bind("milestoneId") int milestoneId, @Bind("fromDate") Date fromDate);

	@Query("(SELECT CARD_LABEL_VALUE_LIST_VALUE_FK, BOARD_COLUMN_DEFINITION_VALUE, COUNT(BOARD_COLUMN_DEFINITION_VALUE) AS MILESTONE_COUNT FROM LA_CARD "
			+ "INNER JOIN LA_BOARD_COLUMN ON CARD_BOARD_COLUMN_ID_FK = BOARD_COLUMN_ID AND BOARD_COLUMN_LOCATION <> 'TRASH' "
			+ "INNER JOIN LA_BOARD_COLUMN_DEFINITION ON BOARD_COLUMN_DEFINITION_ID_FK = BOARD_COLUMN_DEFINITION_ID "
			+ "INNER JOIN LA_BOARD ON BOARD_ID = BOARD_COLUMN_BOARD_ID_FK AND BOARD_PROJECT_ID_FK = :projectId "
			+ "INNER JOIN LA_CARD_LABEL_VALUE ON CARD_ID = CARD_ID_FK AND CARD_LABEL_VALUE_DELETED <> TRUE "
			+ "INNER JOIN LA_CARD_LABEL ON CARD_LABEL_ID = CARD_LABEL_ID_FK AND CARD_LABEL_NAME = 'MILESTONE' AND CARD_LABEL_DOMAIN = 'SYSTEM' "
			+ "GROUP BY CARD_LABEL_VALUE_LIST_VALUE_FK, BOARD_COLUMN_DEFINITION_VALUE) "
			+ "UNION (SELECT NULL, BOARD_COLUMN_DEFINITION_VALUE, COUNT(BOARD_COLUMN_DEFINITION_VALUE) FROM LA_CARD "
			+ "INNER JOIN LA_BOARD_COLUMN ON CARD_BOARD_COLUMN_ID_FK = BOARD_COLUMN_ID AND BOARD_COLUMN_LOCATION <> 'TRASH' "
			+ "INNER JOIN LA_BOARD_COLUMN_DEFINITION ON BOARD_COLUMN_DEFINITION_ID_FK = BOARD_COLUMN_DEFINITION_ID "
			+ "INNER JOIN LA_BOARD ON BOARD_ID = BOARD_COLUMN_BOARD_ID_FK AND BOARD_PROJECT_ID_FK = :projectId "
			+ "WHERE CARD_ID NOT IN (SELECT CARD_ID_FK AS CARD_ID FROM LA_CARD_LABEL_VALUE "
			+ "INNER JOIN LA_CARD_LABEL ON CARD_LABEL_ID = CARD_LABEL_ID_FK "
			+ "WHERE CARD_LABEL_VALUE_DELETED <> TRUE AND CARD_LABEL_DOMAIN = 'SYSTEM' AND CARD_LABEL_NAME = 'MILESTONE') "
			+ "GROUP BY BOARD_COLUMN_DEFINITION_VALUE) ")
	List<MilestoneCount> findCardsCountByMilestone(@Bind("projectId") int projectId);
}