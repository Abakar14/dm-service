package com.bytmasoft.dm.dto.response;

/**
 * TrashPurgeResponse
 *
 * @author Mahamat Abakar
 * @since 05.01.26
 */
public record TrashPurgeResponse(int deletedCount, int olderThanDays) {

}
