package com.xerxes.msme.partner.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdatePartnerAreasRequest(@NotEmpty List<@NotNull UUID> areaIds) {}
