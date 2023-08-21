import base64

import numpy as np
from PIL import Image


def encoded_frame_to_image(encoded_frame, image_width, image_height):
    frame_string = encoded_frame.decode('utf-8').rstrip('\r\n')
    frame_buffer = base64.b64decode(frame_string)

    np_image = np.frombuffer(frame_buffer, dtype=np.uint8).reshape((image_height, image_width, 4))
    np_image = np_image[:, :, :3] # Remove alpha channel

    image = Image.fromarray(np_image, 'RGB')
    return image.transpose(Image.FLIP_TOP_BOTTOM)