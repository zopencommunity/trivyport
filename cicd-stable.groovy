node('linux') {
  stage ('Poll') {
    checkout([
      $class: 'GitSCM', branches: [[name: '*/main']], extensions: [],
      userRemoteConfigs: [[url: 'https://github.com/zopencommunity/trivyport.git']]])
  }
  stage('Build') {
    build job: 'Port-Pipeline', parameters: [
      string(name: 'PORT_GITHUB_REPO', value: 'https://github.com/zopencommunity/trivyport.git'),
      string(name: 'PORT_DESCRIPTION', value: 'Vulnerability scanner for container images, file systems, and Git repos'),
      string(name: 'BUILD_LINE', value: 'STABLE')
    ]
  }
}
