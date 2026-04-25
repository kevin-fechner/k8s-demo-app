package com.demo.notificationservice.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaHealthIndicatorTest {

    @Mock
    private AdminClient adminClient;

    @InjectMocks
    private KafkaHealthIndicator healthIndicator;

    @Test
    @DisplayName("Should return UP with cluster details when Kafka is reachable")
    @SuppressWarnings("unchecked")
    void health_KafkaReachable_ReturnsUp() throws Exception {
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);
        KafkaFuture<Collection<Node>> nodesFuture = mock(KafkaFuture.class);

        when(adminClient.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.clusterId()).thenReturn(clusterIdFuture);
        when(clusterResult.nodes()).thenReturn(nodesFuture);
        when(clusterIdFuture.get(5, TimeUnit.SECONDS)).thenReturn("cluster-abc");
        when(nodesFuture.get(5, TimeUnit.SECONDS)).thenReturn(List.of(mock(Node.class), mock(Node.class)));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("clusterId", "cluster-abc");
        assertThat(health.getDetails()).containsEntry("nodeCount", 2);
    }

    @Test
    @DisplayName("Should return DOWN when Kafka cluster throws an exception")
    @SuppressWarnings("unchecked")
    void health_KafkaUnreachable_ReturnsDown() throws Exception {
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);

        when(adminClient.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.clusterId()).thenReturn(clusterIdFuture);
        when(clusterIdFuture.get(5, TimeUnit.SECONDS)).thenThrow(new RuntimeException("Connection refused"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("Should return DOWN and restore interrupt flag when interrupted")
    @SuppressWarnings("unchecked")
    void health_Interrupted_ReturnsDownAndRestoresInterruptFlag() throws Exception {
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        KafkaFuture<String> clusterIdFuture = mock(KafkaFuture.class);

        when(adminClient.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.clusterId()).thenReturn(clusterIdFuture);
        when(clusterIdFuture.get(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }
}
