package by.mashnyuk.paymentServiceDemo.controller;

import by.mashnyuk.paymentServiceDemo.mcp.ClientToolsMcp;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.LimitResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mcp/tools")
@RequiredArgsConstructor
@Tag(name = "AI MCP Tools", description = "Endpoints exposed as tools for AI Agents")
public class McpToolController {

    private final ClientToolsMcp clientToolsMcp;

    @GetMapping("/client-limits")
    @Operation(summary = "MCP Tool: Get client limits")
    public List<LimitResponseDto> getClientLimits(@RequestParam("account") String account) {
        return clientToolsMcp.getClientLimits(account);
    }

    @GetMapping("/exceeded-transactions")
    @Operation(summary = "MCP Tool: Get exceeded transactions")
    public List<ExceededTransactionResponseDto> getExceededTransactions(@RequestParam("account") String account) {
        return clientToolsMcp.getExceededTransactions(account);
    }
}