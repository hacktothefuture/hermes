/*
 * main.c
 * Sets up Window, AppMessage and a TextLayer to show the message received.
 */

#include <pebble.h>

#define BEARING_DATA 0
#define DIST_DATA 1

static Window *s_main_window;
static TextLayer *s_output_layer;
static TextLayer *title_layer;
static TextLayer *dist_t_layer;
static TextLayer *bearing_t_layer;
static TextLayer *dist_layer;
static TextLayer *bearing_layer;

static char b_buffer[64];
static char d_buffer[64];


static void inbox_received_callback(DictionaryIterator *iterator, void *context) {
  // Get the first pair
  Tuple *t = dict_read_first(iterator);

  // Process all pairs present
  while (t != NULL) {
    // Long lived buffer

    // Process this pair's key
    switch (t->key) {

      case BEARING_DATA:
        // Copy value and display
        snprintf(b_buffer, sizeof(b_buffer), "%s", t->value->cstring);
        text_layer_set_text(bearing_layer, b_buffer);
        break;
      case DIST_DATA:
        // Copy value and display
        snprintf(d_buffer, sizeof(d_buffer), "%s", t->value->cstring);
        text_layer_set_text(dist_layer, d_buffer);
        break;
    }

    // Get next pair, if any
    t = dict_read_next(iterator);
  }
}

static void inbox_dropped_callback(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}

static void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed!");
}

static void outbox_sent_callback(DictionaryIterator *iterator, void *context) {
  APP_LOG(APP_LOG_LEVEL_INFO, "Outbox send success!");
}

static void main_window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect window_bounds = layer_get_bounds(window_layer);

  // Create output TextLayer
  s_output_layer = text_layer_create(GRect(5, 70, window_bounds.size.w - 5, window_bounds.size.h));
  text_layer_set_font(s_output_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  text_layer_set_text(s_output_layer, "Waiting...");
  text_layer_set_overflow_mode(s_output_layer, GTextOverflowModeWordWrap);
//  layer_add_child(window_layer, text_layer_get_layer(s_output_layer));

  title_layer = text_layer_create(GRect(0, 0, window_bounds.size.w-1, 40));
  text_layer_set_font(title_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  text_layer_set_text(title_layer, "Waypoint");
  text_layer_set_overflow_mode(title_layer, GTextOverflowModeWordWrap);
  text_layer_set_text_alignment(title_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(title_layer));

  dist_t_layer = text_layer_create(GRect(0, 40, window_bounds.size.w - 5, 27));
  text_layer_set_font(dist_t_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
  text_layer_set_text(dist_t_layer, "Distance:");
  text_layer_set_overflow_mode(dist_t_layer, GTextOverflowModeWordWrap);
  text_layer_set_text_alignment(dist_t_layer, GTextAlignmentLeft);
  layer_add_child(window_layer, text_layer_get_layer(dist_t_layer));

  dist_layer = text_layer_create(GRect(0, 67, window_bounds.size.w, 27));
  text_layer_set_font(dist_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24));
  text_layer_set_text(dist_layer, "Waiting...");
  text_layer_set_overflow_mode(dist_layer, GTextOverflowModeWordWrap);
  text_layer_set_text_alignment(dist_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(dist_layer));

  bearing_t_layer = text_layer_create(GRect(0, 94, window_bounds.size.w - 1, 27));
  text_layer_set_font(bearing_t_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
  text_layer_set_text(bearing_t_layer, "Bearing:");
  text_layer_set_overflow_mode(bearing_t_layer, GTextOverflowModeWordWrap);
  text_layer_set_text_alignment(bearing_t_layer, GTextAlignmentLeft);
  layer_add_child(window_layer, text_layer_get_layer(bearing_t_layer));

  bearing_layer = text_layer_create(GRect(0, 121, window_bounds.size.w, 27));
  text_layer_set_font(bearing_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24));
  text_layer_set_text(bearing_layer, "Waiting");
  text_layer_set_overflow_mode(bearing_layer, GTextOverflowModeWordWrap);
  text_layer_set_text_alignment(bearing_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(bearing_layer));
  
}

static void main_window_unload(Window *window) {
  // Destroy output TextLayer
  text_layer_destroy(s_output_layer);
  text_layer_destroy(title_layer);
  text_layer_destroy(bearing_t_layer);
  text_layer_destroy(dist_t_layer);
  text_layer_destroy(dist_layer);
  text_layer_destroy(bearing_layer);
}

static void init() {
  // Register callbacks
  app_message_register_inbox_received(inbox_received_callback);
  app_message_register_inbox_dropped(inbox_dropped_callback);
  app_message_register_outbox_failed(outbox_failed_callback);
  app_message_register_outbox_sent(outbox_sent_callback);

  // Open AppMessage
  app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());

  // Create main Window
  s_main_window = window_create();
  window_set_window_handlers(s_main_window, (WindowHandlers) {
    .load = main_window_load,
    .unload = main_window_unload
  });
  window_stack_push(s_main_window, true);
}

static void deinit() {
  // Destroy main Window
  window_destroy(s_main_window);
}

int main(void) {
  init();
  app_event_loop();
  deinit();
}
