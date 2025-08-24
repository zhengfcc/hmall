package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: zhengfeng
 * @Date: 2025/8/24 16:57
 * @Description: 改造下单功能，将基于OpenFeign的清理购物车同步调用，改为基于RabbitMQ的异步通知
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusListener {

    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "cart.clear.queue"),
            exchange = @Exchange(name = "trade.topic"),
            key = "order.create"
    ))
    public void listenCreateOrderSuccess(List<Long> ids){
        cartService.removeByItemIds(ids);
    }
}
