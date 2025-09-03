package core.ms.order_book.web.controllers;

import core.ms.order_book.application.dto.query.CandlestickDTO;
import core.ms.order_book.application.services.CandlestickService;
import core.ms.utils.TimeInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/charts")
@CrossOrigin(origins = "*") // For Vue.js frontend
public class ChartDataController {

    @Autowired
    private CandlestickService candlestickService;

    @GetMapping("/ohlc/{symbol}")
    public List<CandlestickDTO> getOHLCData(
            @PathVariable String symbol,
            @RequestParam String interval,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (to == null) {
            to = LocalDateTime.now();
        }

        TimeInterval timeInterval = TimeInterval.valueOf(interval.toUpperCase());
        return candlestickService.getCandlesticks(symbol, timeInterval, from, to);
    }
}