# -*- coding: utf-8 -*-
from random import randint
from datetime import datetime
import RPi.GPIO as GPIO
import paho.mqtt.client as mqtt
import sys
import time

# Definição do horário de refeição
refeicao=10

# Retira avisos
GPIO.setwarnings(False)

# Definição dos pinos
GPIO.setmode(GPIO.BOARD)
TRIGGER = 7
ECHO = 11
GEAR = 40

GPIO.setup(TRIGGER, GPIO.OUT)
GPIO.setup(ECHO, GPIO.IN)
GPIO.setup(GEAR, GPIO.OUT)

GPIO.output(40,0)

# Verifica a distância entre o sensor e a ração no reservatório
def reservatorio():
	GPIO.output(TRIGGER, GPIO.LOW)
	time.sleep(0.5)

	GPIO.output(TRIGGER, GPIO.HIGH)
	time.sleep(0.00001)
	GPIO.output(TRIGGER, GPIO.LOW)

	while True:
		pulse_start_time = time.time()
		if GPIO.input(ECHO)==GPIO.HIGH:
			break

	while True:
		pulse_end_time = time.time()
		if GPIO.input(ECHO)==GPIO.LOW:
			break
	pulse_duration=pulse_end_time-pulse_start_time
	distance=(34300*pulse_duration)/2

	# Retorna a distância em formato inteiro
	return int(distance)

def ligarMotor(seconds):
	GPIO.output(40,1)
	msg="on"
	client.publish(psm,msg,qos=0)
	time.sleep(seconds)
	GPIO.output(40,0)
	msg="off"
	client.publish(psm,msg,qos=0)

def on_message(client, userdata, msg):
	MensagemRecebida = str(msg.payload)
	MensagemRecebida = MensagemRecebida.strip("\'b")
	print(psa + "/" + str(MensagemRecebida))

	print("[MSG RECEBIDA] Topico: "+msg.topic+" / Mensagem: "+MensagemRecebida.strip("\'b"))

	if MensagemRecebida=="alimentar":
		ligarMotor(60)

# topicos providos por este sensor
psd = "projeto/sensor/distancia"
psm = "projeto/sensor/motor"
psa = "projeto/sensor/alimentar"

# cria um identificador baseado no id do sensor
client = mqtt.Client(client_id = 'NODE:5000-10', protocol = mqtt.MQTTv31)

# conecta no broker
client.connect("broker.hivemq.com", 1883)

try:
	while True:
		# verifica se o reservatorio está cheio e envia a mensagem
		if reservatorio() > 50:
			msg = "acabando"
			client.publish(psd,msg,qos=0)
			print(psd + "/" + str(msg))
		else: # Necessário para que não fique avisando o usuário mesmo depois de completar o reservatorio
			msg = "cheio"
			client.publish(psd,msg,qos=0)
			print(psd + "/" + str(msg))

		# Verificar a hora e liga o motor caso necessite
		now=datetime.now() # data e hora atual
		hora = int(now.strftime('%H'))
		minuto = int(now.strftime('%M'))
		if hora == refeicao:
			if refeicao == 10:
				refeicao = 18 # definição de um novo horário para alimentar o pet
			else:
				refeicao = 10 # definição de um novo horário para alimentar o pet
			ligarMotor(60)

		# Verifica se há mensagem para alimentar o pet
		client.loop_start()
		client.subscribe(psa)
		client.on_message = on_message

		# Tempo de espera para iniciar o loop novamente
		time.sleep(3)
except KeyboardInterrupt:
	print("\nCtrl+C pressionado, encerrando aplicacao e saindo...")
	client.disconnect()
	GPIO.cleanup()
	sys.exit(0)
