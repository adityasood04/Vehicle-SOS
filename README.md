### Emergency SOS message generator app for vehicle crashes.

In this we will connect the app to the MPU6050 sensor to get the angles with magnetic x axis and y axis of earth. This sensor will be installed in vehicles. These values will be transfered to the app using ESP32 wifi module after every 5 seconds. 
If the angle exceeds the abnormal value of inclination of the vehicle i.e. 60 degrees then the app will automatically send a SOS message to the emergency number saved by the user.

Screenshots:<br/>

![EcoBeach](https://github.com/adityasood04/car-sos/assets/98453503/c37734fa-672d-4360-9d1f-b9a575e3e67c)
