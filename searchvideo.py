# Created by xuwu @USC

import numpy as np
from matplotlib import pyplot as plt
import matplotlib.image as mpimg
import cv2
import cv2.cv as cv
import os
import json
import math
import sys
import wave
import struct
from scipy.io.wavfile import read


WIDTH=352
HEIGHT=288

def nparrayTo1Dlist(nparray):
	templist=[]
	size=nparray.size
	nparray=nparray.reshape(1,size).tolist()
	templist=nparray[0]
	return templist

def readRGBfile(filename):
	pixel_nparray=np.zeros((HEIGHT,WIDTH,3),np.uint8)
	index=0
	numpy_array=np.fromfile(filename,dtype=np.uint8)
	for j in xrange(0,HEIGHT):
		for i in xrange(0,WIDTH):
			r=numpy_array[index]
			g=numpy_array[index+HEIGHT*WIDTH]
			b=numpy_array[index+2*HEIGHT*WIDTH]
			pixel_nparray[j][i]=[r,g,b]
			index=index+1
			
	return pixel_nparray

def get_kmeans_center(filename):
	result=[]
	pixel_nparray=readRGBfile(filename)
	z=pixel_nparray.reshape(1,HEIGHT*WIDTH,3)
	z=np.float32(z)
	criteria=(cv2.TERM_CRITERIA_EPS+cv2.TERM_CRITERIA_MAX_ITER,10,1.0)
	ret,label,center=cv2.kmeans(z,1,criteria,10,cv2.KMEANS_RANDOM_CENTERS)
	result=center.tolist()
	return result[0]



def convert_mvframesTo_gray(framename):
	pixel_nparray3=readRGBfile(framename)
	gray_image=cv2.cvtColor(pixel_nparray3, cv2.COLOR_RGB2GRAY)		
	return gray_image

def diffImg(t0, t1, t2):
	d1 = cv2.absdiff(t2, t1)
	d2 = cv2.absdiff(t1, t0)
	return cv2.bitwise_and(d1, d2)




def diff_detect(foldername):
	result_domain_centers=[]
	result_motion_diff=[]
	temp1=[]
	temp2=[]
	framenames=[]
	temp_framenames=os.listdir(foldername)
	for filenamex in temp_framenames:
		if ".rgb" in filenamex:
			framenames.append(filenamex)

	gray_curleft=convert_mvframesTo_gray(foldername+framenames[0])
	gray_curmid=convert_mvframesTo_gray(foldername+framenames[1])
	gray_curright=convert_mvframesTo_gray(foldername+framenames[2])
	result_domain_centers.append(get_kmeans_center(foldername+framenames[0]))
	temp1=nparrayTo1Dlist(cv2.absdiff(convert_mvframesTo_gray(foldername+framenames[1]),convert_mvframesTo_gray(foldername+framenames[0])))
	temp1=[sum(temp1)]
	result_motion_diff.append(temp1)
	temp2=nparrayTo1Dlist(diffImg(gray_curleft,gray_curmid,gray_curright))
	temp2=[sum(temp2)]
	result_motion_diff.append(temp2)



	for i in xrange(1,len(framenames)-2):
		temp3=[]
		temp4=[]
		result_domain_centers.append(get_kmeans_center(foldername+framenames[i]))
		gray_curleft=gray_curmid
		gray_curmid=gray_curright
		gray_curright=convert_mvframesTo_gray(foldername+framenames[i+2])
		temp3=nparrayTo1Dlist(diffImg(gray_curleft,gray_curmid,gray_curright))
		temp3=[sum(temp3)]
		result_motion_diff.append(temp3)


	result_domain_centers.append(get_kmeans_center(foldername+framenames[len(framenames)-1]))
	result_domain_centers.append(get_kmeans_center(foldername+framenames[len(framenames)-2]))
	temp4=nparrayTo1Dlist(cv2.absdiff(convert_mvframesTo_gray(foldername+framenames[len(framenames)-1]),convert_mvframesTo_gray(foldername+framenames[len(framenames)-2])))
	temp4=[sum(temp4)]

	result_motion_diff.append(temp4)


	return result_domain_centers,result_motion_diff



def caculate_normdis(list1,list2):

	dist=np.linalg.norm(list1-list2)
	return dist


def cac_match(query_centers,query_motions,database_centers,database_motions):
	list_diff=[]
	index1=0
	index2=0
	x1=np.array(query_centers)
	x2=np.array(query_motions)
	y1=np.array(database_centers)
	y2=np.array(database_motions)
	for i in xrange(0,450):
		center_rootdiff_sum=0
		motion_rootdiff_sum=0
		for j in xrange(0,150):
			list1=x1[j]
			list2=y1[j+i]
			rootdiff1=caculate_normdis(list1,list2)
			center_rootdiff_sum=center_rootdiff_sum+rootdiff1
			list3=x2[j]
			list4=y2[j+i]
			rootdiff2=caculate_normdis(list3,list4)
			motion_rootdiff_sum=motion_rootdiff_sum+rootdiff2
		temp1=round(center_rootdiff_sum/150,2)
		temp2=round(motion_rootdiff_sum/150,2)
		list_diff.append([temp1,temp2])
		# print list_diff[i]
		# print i
	max_color=list_diff[0][0]
	min_color=list_diff[0][0]
	max_motion=list_diff[0][1]
	min_motion=list_diff[0][1]
	for k in list_diff:
		if k[0]>max_color:
			max_color=k[0]
		if k[0]<min_color:
			min_color=k[0]
			index1=list_diff.index(k)

		if k[1]>max_motion:
			max_motion=k[1]
		if k[1]<min_motion:
			min_motion=k[1]
			index2=list_diff.index(k)

	rangelist_color=[min_color,max_color]
	rangelist_motion=[min_motion,max_motion]
	
	return index1,index2,list_diff,rangelist_color,rangelist_motion

def get_score(result,weight):
	minIndex = 0
	score_result=[]
	for index in xrange(1, len(result)):
		if result[minIndex] > result[index]:
			minIndex = index

	minResult = result[minIndex]

	for index in xrange(0, len(result)):
		score_result.append(round(minResult * weight / result[index]))
	return score_result




foldernames_list=["flowers/","interview/","movie/","musicvideo/","sports/","starcraft/","traffic/"]

cur_dir=os.listdir(".")
databasefile="save.json"
if databasefile not in cur_dir:
	to_save={}
	for foldername in foldernames_list:
		domain_centers=[]
		motion_diff=[]
		domain_centers,motion_diff=diff_detect(foldername)
		folder_dict={}
		folder_dict["centers"]=domain_centers
		folder_dict["motions"]=motion_diff
		to_save[foldername]=folder_dict
	with open('save.json', 'w') as output_file:
		json.dump(to_save, output_file)
else:
	
	print "Processing query sample data..."

txt_list=[]
txt_list.append(sys.argv[2])
txt_list.append(sys.argv[3])
audio_queryname=txt_list[1]
srt=txt_list[0].split('.')
query_foldername=srt[0]
query_name=query_foldername[:-3]
query_foldername="query/"+query_name+"/"


queryresult_name=query_name+"_score.txt"
if queryresult_name not in cur_dir:
                          
	with open('save.json') as input_file:
		dicts=json.loads(input_file.read())

	query_centers=[]
	query_motions=[]
	query_centers,query_motions=diff_detect(query_foldername)

	result_color=[]
	result_motion=[]
	result_position=[]

	for foldername in foldernames_list:

		database_centers=[]
		database_motions=[]

		database_centers=dicts[foldername]["centers"]
		database_motions=dicts[foldername]["motions"]
		list_diff=[]
		rangelist_color=[]
		rangelist_motion=[]
		index1,index2,list_diff,rangelist_color,rangelist_motion=cac_match(query_centers,query_motions,database_centers,database_motions)
		result_color.append(rangelist_color[0])
		result_motion.append(rangelist_motion[0])
		result_position.append(index1)



	audio_nameList = ["flowers/flowers.wav", "interview/interview.wav", "movie/movie.wav", "musicvideo/musicvideo.wav", "sports/sports.wav", "StarCraft/StarCraft.wav", "traffic/traffic.wav"]
	audio_result = []
	audio_score=[]

	for idx in range(0, 7):

	    rate, data = read(audio_nameList[idx])
	    data = data / (2.**15)
	    data0 = data[:, 0]
	    f = np.fft.fft(data0, 131072)
	    data_mag = np.abs(f)
	    


	    rate, sample = read(query_foldername+audio_queryname)
	    sample = sample / (2.**15)
	    sample0 = sample[:, 0]
	    f = np.fft.fft(sample0, 131072)
	    sample_mag = np.abs(f)

	    ratio = []

	    for index in range(0, 50000):
	        if data_mag[index] == 0:
	            ratio.append(1)
	        else:
	            ratio.append(sample_mag[index] / data_mag[index])


	    N = len(ratio)
	    narray = np.array(ratio)
	    sum1 = narray.sum()
	    narray2 = narray * narray
	    sum2 = narray2.sum()
	    mean = sum1 / N
	    var = sum2 / N - mean**2
	    audio_result.append(var)
		

	ff=open(queryresult_name,"w")
	color_score=[]
	motion_score=[]
	color_score=get_score(result_color,60)
	motion_score=get_score(result_motion,25)
	audio_score=get_score(audio_result,15)
	print color_score
	print motion_score
	print audio_score
	output = []
	for i in xrange(0,len(foldernames_list)):
		output.append((foldernames_list[i][:-1], color_score[i]+motion_score[i]+audio_score[i], result_position[i]+1))
	output = sorted(output, key = lambda x: -x[1])
	for line in output:
		print>>ff, ','.join(map(str, line))
print "Done! Open UI to check the result!"




