public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

OkHttpClient client = new OkHttpClient();

Call post(String url, String json, Callback callback) throws IOException {
  RequestBody body = RequestBody.create(JSON, json);
  Request request = new Request.Builder()
      .url(url)
      .post(body)
      .build();
  Call call = client.newCall(request)
  call.enqueue(callback);
  return call;
}

post("http://www.roundsapp.com/post", json, new Callback() {
  @Override
  public void onFailure(Request request, Throwable throwable) {
     // Something went wrong
  }

  @Override public void onResponse(Response response) throws IOException {
    if (response.isSuccessful()) {
       String responseStr = response.body().string();
       // Do what you want to do with the response.
    } else {
       // Request not successful
    }
  }
});