package de.aporz.doctorassistant.controller;

import de.aporz.doctorassistant.dto.QueryRequest;
import de.aporz.doctorassistant.dto.QueryResponse;
import de.aporz.doctorassistant.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@Tag(name = "Query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Process a medical question with LLM1+Vector+LLM2 orchestration")
    @PostMapping
    public ResponseEntity<QueryResponse> ask(@RequestBody QueryRequest request) {
        return ResponseEntity.ok(queryService.handle(request));
    }
}
