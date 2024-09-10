
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <stdatomic.h>
#include <errno.h>

#define PORT 2321
#define BUFFER_SIZE 3000 * 9
#define MESSAGE "Hello 000"
#define CLIENT_CREATION_DELAY 20000 // 20 milliseconds
#define SEND_DELAY 50000 // 50 milliseconds

atomic_ulong total_bytes_received = 0;
	

void* client_thread(void* arg) {
    int sockfd;
    struct sockaddr_in server_addr;
    char buffer[BUFFER_SIZE];
    int len = snprintf(buffer, BUFFER_SIZE, "%s", MESSAGE);
    
    // Create socket
    if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("Socket creation failed");
        return NULL;
    }

    // Prepare the server address
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(PORT);

    // Connect to the server
    if (connect(sockfd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        perror("Connect failed");
        close(sockfd);
        return NULL;
    }

    // Send data
    while (1) {
        if (send(sockfd, buffer, BUFFER_SIZE, 0) < 0) {
            perror("Send failed");
            break;
        }
        usleep(SEND_DELAY); // Sleep for 50 milliseconds
    }

    close(sockfd);
    return NULL;
}

void* processor_thread(void* arg) {
	int sock = *(int*)arg;
	while(1) {
		char buf[8192];
		ssize_t received = recv(sock, buf, sizeof(buf),0);
		if (received < 0) {
			perror("Bad received\n");

			exit(1);
		}
		atomic_fetch_add(&total_bytes_received,received);
	}

}

void* server_thread(void* arg) {
    int server_sockfd, client_sockfd;
    struct sockaddr_in server_addr, client_addr;
    socklen_t client_len;
    char buffer[BUFFER_SIZE];
    ssize_t bytes_received;
    
    // Create server socket
    if ((server_sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("Server socket creation failed");
        return NULL;
    }

    // Prepare the server address
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(PORT);

    // Bind the server socket
    if (bind(server_sockfd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        perror("Bind failed");
        close(server_sockfd);
        return NULL;
    }

    // Listen for connections
    if (listen(server_sockfd, 10) < 0) {
        perror("Listen failed");
        close(server_sockfd);
        return NULL;
    }

    while (1) {
        client_len = sizeof(client_addr);
        if ((client_sockfd = accept(server_sockfd, (struct sockaddr*)&client_addr, &client_len)) < 0) {
            perror("Accept failed");
            continue;
        }

        // Create a new thread to handle this connection
        pthread_t recv_thread;
				void* arg = &client_sockfd;
        if (pthread_create(&recv_thread, NULL, processor_thread, arg) != 0) {
            perror("Thread creation failed");
            close(client_sockfd);
        } else {
					pthread_detach(recv_thread);
				}
    }

    close(server_sockfd);
    return NULL;
}

double cur_time() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
		return tv.tv_sec + (double)(tv.tv_usec)/1000000;
}

void* monitor_thread(void* arg) {
		long last_data=0;
		double last_time = cur_time();
    while (1) {
			sleep(1);
			double now = cur_time();
			long data_now = atomic_load(&total_bytes_received);
			double data_delta = data_now - last_data;
			double rate = data_delta / (now - last_time) / 1024/1024;
			last_time = now;
			last_data=data_now;

			printf("Throughput: %f MiB/second\n", rate);
    }
    return NULL;
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <number_of_streams>\n", argv[0]);
        return EXIT_FAILURE;
    }

    int num_streams = atoi(argv[1]);
    if (num_streams <= 0) {
        fprintf(stderr, "Invalid number of streams\n");
        return EXIT_FAILURE;
    }

    pthread_t server_tid, monitor_tid;

    // Create server thread
    if (pthread_create(&server_tid, NULL, server_thread, NULL) != 0) {
        perror("Server thread creation failed");
        return EXIT_FAILURE;
    }

    // Create monitoring thread
    if (pthread_create(&monitor_tid, NULL, monitor_thread, NULL) != 0) {
        perror("Monitor thread creation failed");
        return EXIT_FAILURE;
    }

    // Create client threads
    pthread_t* client_threads = malloc(num_streams * sizeof(pthread_t));
    if (client_threads == NULL) {
        perror("Memory allocation failed");
        return EXIT_FAILURE;
    }

    for (int i = 0; i < num_streams; ++i) {
        if (pthread_create(&client_threads[i], NULL, client_thread, NULL) != 0) {
            perror("Client thread creation failed");
        } else {
					pthread_detach(client_threads[i]);
					printf("Created client %d\n",i);
					fflush(stdout);
				}
        usleep(CLIENT_CREATION_DELAY); // Sleep for 20 milliseconds between client creations
    }

    // Wait for all threads
    for (int i = 0; i < num_streams; ++i) {
        pthread_join(client_threads[i], NULL);
    }

    free(client_threads);

    // The program does not exit, so we join the server and monitor threads
    pthread_join(server_tid, NULL);
    pthread_join(monitor_tid, NULL);

    return EXIT_SUCCESS;
}

