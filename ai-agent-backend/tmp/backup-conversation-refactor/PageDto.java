package com.xin.aiagentbackend.conversation;

import java.util.List;

public record PageDto<T>(List<T> list, long total, int page, int pageSize) { }
