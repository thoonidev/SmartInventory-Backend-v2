package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.LiveEventDTO;

public interface LiveEventService {
    void publish(Long organizationId, LiveEventDTO event);
}
