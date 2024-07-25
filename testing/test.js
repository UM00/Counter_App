const axios = require('axios');

const config = {
  method: 'post',
  url: 'http://localhost:8081/lockWithRedis',
};

async function runTests() {
  console.time('Execution Time');
  const requests = [];
  
  for (let i = 0; i < 1000; i++) {
    requests.push(
      axios(config)
        .then(response => console.log( response.data))
        .catch(error => console.log(error))
    );
  }
  
  await Promise.all(requests);
  console.log('All requests completed.');
  console.timeEnd('Execution Time');

}

runTests();
